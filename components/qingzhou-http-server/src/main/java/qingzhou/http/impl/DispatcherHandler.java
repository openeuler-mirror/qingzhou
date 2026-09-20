package qingzhou.http.impl;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.reactivestreams.Publisher;
import qingzhou.http.server.BodyTooLargeException;
import qingzhou.http.server.HttpHandler;
import qingzhou.logger.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.concurrent.Queues;

import static qingzhou.http.impl.HttpServerImpl.getConfig;

@Component(configurationPid = "qingzhou-http-server", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = DispatcherHandler.class)
public class DispatcherHandler implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {
    @Reference
    private Logger logger;
    @Reference
    private AuthManager authManager;
    @Reference
    private HandlerManager handlerManager;

    private final byte[] NULL_BYTES = new byte[0];
    private Semaphore concurrentSemaphore;
    private boolean isSslEnabled;
    private long maxStreamBytes; // 流式上传总量上限
    private int maxBodyBytes; // 单个请求体聚合进内存的上限
    private String csp;

    @Activate
    public void init(Map<String, String> config) {
        int maxConcurrentRequests = getConfig(config, "max_concurrent_requests", 1000);
        if (maxConcurrentRequests <= 0) throw new IllegalArgumentException("max_concurrent_requests must be positive");
        this.concurrentSemaphore = new Semaphore(maxConcurrentRequests);

        isSslEnabled = getConfig(config, "ssl_enabled", true);

        maxStreamBytes = getConfig(config, "max_stream_bytes", 1024L * 1024 * 1024);
        maxBodyBytes = getConfig(config, "max_body_bytes", 8 * 1024 * 1024);
        csp = getConfig(config, "csp", "none");
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        addSecurityHeaders(response); // 须在任何 return 之前：400/401/404/413/503 等分支同样需要安全头
        if (!concurrentSemaphore.tryAcquire()) { // 请求体按上限聚合进内存，不限在途请求数则内存仍会耗尽
            return respondAndClose(response, HttpResponseStatus.SERVICE_UNAVAILABLE);
        }
        // 延迟执行：分发逻辑同步抛异常时也能走到 doFinally，不至于泄漏许可
        return Flux.defer(() -> dispatch(request, response)).doFinally(signal -> concurrentSemaphore.release());
    }

    private Publisher<Void> dispatch(HttpServerRequest request, HttpServerResponse response) {
        String uri = request.uri();
        int queryIndex = uri.indexOf('?');
        String requestPath = queryIndex < 0 ? uri : uri.substring(0, queryIndex);
        try {
            requestPath = URLDecoder.decode(requestPath, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return reject(request, response, HttpResponseStatus.BAD_REQUEST);
        }
        if (isDoubleEncoded(requestPath)) { // %252e%252e 解码一次后仍是 %2e%2e，再解一次即变成 ".."，一律拒绝
            return reject(request, response, HttpResponseStatus.BAD_REQUEST);
        }
        requestPath = normalize(requestPath);
        if (requestPath == null) { // 含 ".." 段一律拒绝：handler 各自解析路径，无法保证都能防住穿越
            return reject(request, response, HttpResponseStatus.BAD_REQUEST);
        }
        HttpRequestImpl httpRequest = new HttpRequestImpl(request, requestPath);

        HttpHandler httpHandler = handlerManager.findHandler(requestPath);
        if (httpHandler == null) {
            return reject(request, response, HttpResponseStatus.NOT_FOUND);
        }

        // 安全认证
        boolean doneAuth = authManager.doAuth(httpRequest, httpHandler);
        if (!doneAuth) {
            return reject(request, response
                            .header(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_STORE),
                    HttpResponseStatus.UNAUTHORIZED);
        }

        // 开始处理业务...
        HttpHandler.StreamHandler streamHandler;
        try {
            streamHandler = httpHandler.multipartStreamHandler();
        } catch (Throwable e) {
            return rejectHandlerError(request, response, e);
        }
        boolean streamRequired = request.method() == HttpMethod.POST && request.isMultipart();
        if (streamRequired && streamHandler == null) {
            return reject(request, response, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        int STREAM_QUEUE_SIZE = 1024; // 慢客户端时最多缓存的响应分片数
        Sinks.Many<byte[]> streamResponse = Sinks.many().unicast()
                .onBackpressureBuffer(Queues.<byte[]>get(STREAM_QUEUE_SIZE).get()); // 有界缓冲：慢客户端不再导致内存无界堆积
        HttpResponseImpl httpResponse = new HttpResponseImpl(response, streamResponse);

        if (streamRequired) {
            if (bodyTooLarge(request, maxStreamBytes)) { // 预检：超限直接拒，不必先落临时文件
                return reject(request, response, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
            }
            try {
                streamHandler.onBegin(httpRequest, httpResponse);
            } catch (Throwable e) {
                return rejectHandlerError(request, response, e);
            }
            AtomicLong receivedBytes = new AtomicLong();
            request.receive() // 下面开始 直接订阅原始数据流，不进行 聚合
                    .doOnNext(byteBuf -> { // 限制上传总量，超限触发 onError，由 handler 清理临时文件并回错误
                        if (receivedBytes.addAndGet(byteBuf.readableBytes()) > maxStreamBytes) {
                            throw new BodyTooLargeException();
                        }
                    })
                    .subscribe(byteBuf -> {
                                byte[] bytes = new byte[byteBuf.readableBytes()];
                                byteBuf.readBytes(bytes);
                                streamHandler.onNext(bytes);

                                // ByteBuf 由 Reactor Netty 框架负责释放，此处不得手动 release，否则引用计数提前耗尽
                            },
                            err -> {
                                streamHandler.onError(err);
                                // 兜底：handler 若未发响应就返回，响应链永不结束，请求会一直挂到超时
                                if (!httpResponse.isUsed()) {
                                    logger.error("http stream handler error", getCause(err));
                                    // 超限回 413、其余回 500，与聚合分支语义一致；响应体不回显内部异常细节
                                    HttpResponseStatus status = statusOf(err);
                                    httpResponse.status(status.code()).sendFinish(status.reasonPhrase());
                                }
                            },
                            streamHandler::onComplete // 完成信号
                    );
            return response.sendByteArray(streamResponse.asFlux()).then();
        } else {
            if (bodyTooLarge(request, maxBodyBytes)) {
                return reject(request, response, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
            }
            AtomicLong receivedBytes = new AtomicLong();
            return ByteBufFlux.fromInbound(request.receive()
                            .doOnNext(byteBuf -> { // chunked 请求无 Content-Length，聚合前按实际字节数二次限制
                                if (receivedBytes.addAndGet(byteBuf.readableBytes()) > maxBodyBytes) {
                                    throw new BodyTooLargeException();
                                }
                            }))
                    .aggregate().asByteArray() // 所有输入 聚合 到一起再发送给订阅者
                    .defaultIfEmpty(NULL_BYTES)
                    .flatMap(bytes -> {
                        try {
                            httpRequest.setRequestBody(bytes);
                            httpHandler.handle(httpRequest, httpResponse);
                            if (!httpResponse.isUsed()) {
                                streamResponse.tryEmitComplete(); // 避免请求无限等
                            }
                        } catch (Throwable e) {
                            logger.error("http handler error", getCause(e));
                            if (httpResponse.isUsed()) {
                                streamResponse.tryEmitComplete(); // 已写过响应，状态码无法回退，只能干净收尾
                            } else {
                                HttpResponseStatus status = statusOf(e);
                                httpResponse.status(status.code()).sendFinish(status.reasonPhrase());
                            }
                        }
                        return response.sendByteArray(streamResponse.asFlux()).then();
                    })
                    .onErrorResume(e -> respondAndClose(response, statusOf(e))); // 连接中断等也应回响应，且不能一律报 413
        }
    }

    /**
     * 发错误响应前先读完请求体：保活连接上残留的请求体会被当作下一个请求解析（请求走私）。
     * 读取体量同样受限，避免错误分支被当成免费上传通道。
     */
    private Publisher<Void> reject(HttpServerRequest request, HttpServerResponse response, HttpResponseStatus status) {
        Mono<Void> errorResponse = response.status(status).send();
        AtomicLong receivedBytes = new AtomicLong();
        return request.receive()
                .doOnNext(byteBuf -> {
                    if (receivedBytes.addAndGet(byteBuf.readableBytes()) > maxBodyBytes) {
                        throw new BodyTooLargeException();
                    }
                })
                .then(errorResponse)
                .onErrorResume(e -> respondAndClose(response, statusOf(e)));
    }

    // handler 回调同步抛出时请求体还没读完，须先排空再回 500，否则残留字节会被当作下一个请求解析
    private Publisher<Void> rejectHandlerError(HttpServerRequest request, HttpServerResponse response, Throwable e) {
        logger.error("http handler error", getCause(e));
        return reject(request, response, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    // 请求体未读完，连接无法复用：发送响应并关闭连接，避免残留字节污染后续请求
    private Mono<Void> respondAndClose(HttpServerResponse response, HttpResponseStatus status) {
        return response
                .status(status)
                .header(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                .send();
    }

    /**
     * 路径规范化：折叠重复斜杠与 "." 段；发现 ".." 段返回 null（拒绝请求）。
     * 各 handler 自行解析路径，防护难以统一，故在分发入口一次性拦截穿越。
     */
    private String normalize(String path) {
        StringBuilder normalized = new StringBuilder(path.length());
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || segment.equals(".")) continue;
            if (segment.equals("..")) return null;
            normalized.append('/').append(segment);
        }
        if (path.endsWith("/") && normalized.length() > 0) normalized.append('/');
        return normalized.length() == 0 ? "/" : normalized.toString();
    }

    // 一次解码后仍保留编码形态，说明原始路径被二次编码（如 %252e%252e），各 handler 的穿越防护未必覆盖得住
    private boolean isDoubleEncoded(String path) {
        String upper = path.toUpperCase(Locale.ROOT);
        return upper.contains("%2E") || upper.contains("%25");
    }

    // Content-Length 预检给出干净的 413；非法头交给字节计数兜底
    private boolean bodyTooLarge(HttpServerRequest request, long limit) {
        String value = request.requestHeaders().get(HttpHeaderNames.CONTENT_LENGTH);
        if (value == null) return false;
        try {
            return Long.parseLong(value) > limit;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // 异常可能被 Reactor 包装，取最内层 cause 判断
    private HttpResponseStatus statusOf(Throwable e) {
        return getCause(e) instanceof BodyTooLargeException
                ? HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE
                : HttpResponseStatus.INTERNAL_SERVER_ERROR;
    }

    private void addSecurityHeaders(HttpServerResponse response) {
        response.header("X-Content-Type-Options", "nosniff")
                .header("X-Frame-Options", "SAMEORIGIN")
                .header("Referrer-Policy", "no-referrer");
        if (csp != null && !csp.isEmpty() && !csp.equals("none")) {
            response.header("Content-Security-Policy", csp);
        }
        if (isSslEnabled) {
            response.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
    }

    private Throwable getCause(Throwable e) {
        Throwable cause = e;

        while (cause != null
                && cause.getCause() != null
                && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        return cause;
    }
}
