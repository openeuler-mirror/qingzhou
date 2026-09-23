package qingzhou.http.impl;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
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

    private static final byte[] EMPTY_BODY = new byte[0];

    private final AtomicLong aggregatedBytes = new AtomicLong();
    private final List<RequestGate> gates;

    public DispatcherHandler() {
        // 顺序即优先级：读 ctx.path 的须排在 denyUnnormalizedPath 之后，读 ctx.entry 的须排在 denyUnroutedPath 之后
        gates = Arrays.asList(
                this::denyDisallowedMethod, // 405
                this::denyUnnormalizedPath, // 400：解码失败 / 二次编码 / 路径穿越
                this::denyUnroutedPath,     // 404
                this::denyUnauthenticated,  // 401
                this::denyUnsupportedBody,  // 415
                this::denyOversizedBody,    // 413
                this::denyExhaustedMemory   // 503
        );
    }

    // 以下由 OSGi 激活线程写入、Netty EventLoop 读取，须保证可见性
    private volatile Semaphore concurrentSemaphore;
    private volatile boolean isSslEnabled;
    private volatile long maxStreamBytes; // 流式上传总量上限
    private volatile int maxBodyBytes; // 单个请求体聚合进内存的上限
    private volatile long maxAggregatedBytes; // 同时在途的聚合请求体总量上限
    private volatile String csp;

    @Activate
    public void init(Map<String, String> config) {
        int maxConcurrentRequests = getConfig(config, "max_concurrent_requests", 1000);
        if (maxConcurrentRequests <= 0) throw new IllegalArgumentException("max_concurrent_requests must be positive");
        this.concurrentSemaphore = new Semaphore(maxConcurrentRequests);

        isSslEnabled = getConfig(config, "ssl_enabled", true);

        maxStreamBytes = getConfig(config, "max_stream_bytes", 1024L * 1024 * 1024);
        maxBodyBytes = getConfig(config, "max_body_bytes", 8 * 1024 * 1024);
        maxAggregatedBytes = getConfig(config, "max_aggregated_bytes", 64L * 1024 * 1024);
        csp = getConfig(config, "csp", "none");
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        addSecurityHeaders(response); // 须在任何 return 之前：400/401/404/413/503 等分支同样需要安全头
        if (!concurrentSemaphore.tryAcquire()) { // 请求体按上限聚合进内存，不限在途请求数则内存仍会耗尽
            return respondAndClose(response, HttpResponseStatus.SERVICE_UNAVAILABLE);
        }
        RequestContext ctx = new RequestContext(request, response);
        // 延迟执行：分发逻辑同步抛异常时也能走到 doFinally，不至于泄漏许可
        return Flux.defer(() -> dispatch(ctx)).doFinally(signal -> release(ctx));
    }

    // 许可与内存配额统一在入口归还：gate 只管判定，不必关心释放，也不受 gate 顺序影响
    private void release(RequestContext ctx) {
        concurrentSemaphore.release();
        if (ctx.reservedBytes > 0) aggregatedBytes.addAndGet(-ctx.reservedBytes);
    }

    private Publisher<Void> dispatch(RequestContext ctx) {
        for (RequestGate gate : gates) { // 短路：首个拒绝即终止，其后 gate 不再执行
            Publisher<Void> denial = gate.check(ctx);
            if (denial != null) return denial;
        }
        return handleBody(ctx);
    }

    // 405：TRACE 等会落到兜底 handler（注册在 / 的页面），有跨站追踪风险
    private Publisher<Void> denyDisallowedMethod(RequestContext ctx) {
        return isMethodAllowed(ctx.request.method()) ? null
                : reject(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
    }

    // 400：解码失败、二次编码、含 ".." 段，三者在分发入口一并拦截；
    // 各 handler 自行解析路径，防护难以统一，故不在此放行任何可疑形态
    private Publisher<Void> denyUnnormalizedPath(RequestContext ctx) {
        String uri = ctx.request.uri();
        int queryIndex = uri.indexOf('?');
        String path = queryIndex < 0 ? uri : uri.substring(0, queryIndex);
        try {
            path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return reject(ctx, HttpResponseStatus.BAD_REQUEST);
        }
        if (isDoubleEncoded(path)) return reject(ctx, HttpResponseStatus.BAD_REQUEST);
        path = normalize(path);
        if (path == null) return reject(ctx, HttpResponseStatus.BAD_REQUEST);

        ctx.path = path;
        ctx.httpRequest = new HttpRequestImpl(ctx.request, path);
        return null;
    }

    private Publisher<Void> denyUnroutedPath(RequestContext ctx) {
        ctx.entry = handlerManager.findHandler(ctx.path);
        return ctx.entry == null ? reject(ctx, HttpResponseStatus.NOT_FOUND) : null;
    }

    private Publisher<Void> denyUnauthenticated(RequestContext ctx) {
        if (authManager.doAuth(ctx.httpRequest, ctx.entry)) return null;
        return reject(ctx.request, ctx.response.header(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_STORE),
                HttpResponseStatus.UNAUTHORIZED);
    }

    // 415：multipart 必须有 StreamHandler；handler 自身抛出视为 500
    private Publisher<Void> denyUnsupportedBody(RequestContext ctx) {
        ctx.streamRequired = ctx.request.method() == HttpMethod.POST && ctx.request.isMultipart();
        try {
            ctx.streamHandler = ctx.entry.handler.multipartStreamHandler();
        } catch (Throwable e) {
            logger.error("http handler error", getCause(e));
            return reject(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        return ctx.streamRequired && ctx.streamHandler == null
                ? reject(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE) : null;
    }

    // 413：Content-Length 预检，两条通道的上限不同
    private Publisher<Void> denyOversizedBody(RequestContext ctx) {
        long limit = ctx.streamRequired ? maxStreamBytes : maxBodyBytes;
        return bodyTooLarge(ctx.request, limit)
                ? reject(ctx, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE) : null;
    }

    // 503：仅聚合分支吃堆内存；流式由 handler 落临时文件，不占此预算
    private Publisher<Void> denyExhaustedMemory(RequestContext ctx) {
        ctx.reservedBytes = ctx.streamRequired ? 0 : reserveAggregation(ctx.request);
        return ctx.reservedBytes < 0 ? reject(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE) : null;
    }

    private Publisher<Void> handleBody(RequestContext ctx) {
        int queueSize = 1024; // 慢客户端时最多缓存的响应分片数
        Sinks.Many<byte[]> streamResponse = Sinks.many().unicast()
                .onBackpressureBuffer(Queues.<byte[]>get(queueSize).get()); // 有界缓冲：慢客户端不再导致内存无界堆积
        HttpResponseImpl httpResponse = new HttpResponseImpl(ctx.response, streamResponse);
        return ctx.streamRequired ? receiveStream(ctx, httpResponse, streamResponse)
                : aggregate(ctx, httpResponse, streamResponse);
    }

    private Publisher<Void> receiveStream(RequestContext ctx, HttpResponseImpl httpResponse,
                                          Sinks.Many<byte[]> streamResponse) {
        HttpHandler.StreamHandler streamHandler = ctx.streamHandler;
        try {
            streamHandler.onBegin(ctx.httpRequest, httpResponse);
        } catch (Throwable e) {
            return rejectHandlerError(ctx.request, ctx.response, e);
        }
        AtomicLong receivedBytes = new AtomicLong();
        ctx.request.receive() // 下面开始 直接订阅原始数据流，不进行 聚合
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
        return ctx.response.sendByteArray(streamResponse.asFlux()).then();
    }

    private Publisher<Void> aggregate(RequestContext ctx, HttpResponseImpl httpResponse,
                                      Sinks.Many<byte[]> streamResponse) {
        HttpHandler httpHandler = ctx.entry.handler;
        AtomicLong receivedBytes = new AtomicLong();
        return ByteBufFlux.fromInbound(ctx.request.receive()
                        .doOnNext(byteBuf -> { // chunked 请求无 Content-Length，聚合前按实际字节数二次限制
                            if (receivedBytes.addAndGet(byteBuf.readableBytes()) > maxBodyBytes) {
                                throw new BodyTooLargeException();
                            }
                        }))
                .aggregate().asByteArray() // 所有输入 聚合 到一起再发送给订阅者
                .defaultIfEmpty(EMPTY_BODY)
                .flatMap(bytes -> {
                    try {
                        ctx.httpRequest.setRequestBody(bytes);
                        httpHandler.handle(ctx.httpRequest, httpResponse);
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
                    return ctx.response.sendByteArray(streamResponse.asFlux()).then();
                })
                .onErrorResume(e -> respondAndClose(ctx.response, statusOf(e))); // 连接中断等也应回响应，且不能一律报 413
    }

    private Publisher<Void> reject(RequestContext ctx, HttpResponseStatus status) {
        return reject(ctx.request, ctx.response, status);
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
        return declaredBodySize(request) > limit;
    }

    // 无请求体返回 0；chunked（长度未知）返回 -1；非法 Content-Length 返回 0，交由字节计数兜底
    private long declaredBodySize(HttpServerRequest request) {
        String value = request.requestHeaders().get(HttpHeaderNames.CONTENT_LENGTH);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return request.requestHeaders()
                .contains(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED, true) ? -1 : 0;
    }

    // 返回预留的字节数；-1 表示在途聚合总量已达上限
    private long reserveAggregation(HttpServerRequest request) {
        long size = declaredBodySize(request);
        if (size < 0) size = maxBodyBytes; // 仅 chunked 长度未知，按单体上限保守预留
        while (true) {
            long current = aggregatedBytes.get();
            if (current + size > maxAggregatedBytes) return -1;
            if (aggregatedBytes.compareAndSet(current, current + size)) return size;
        }
    }

    // TRACE 等非业务方法不应进入任何 handler
    private static boolean isMethodAllowed(HttpMethod method) {
        return method == HttpMethod.GET || method == HttpMethod.POST || method == HttpMethod.PUT
                || method == HttpMethod.DELETE || method == HttpMethod.PATCH || method == HttpMethod.HEAD
                || method == HttpMethod.OPTIONS;
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

    /**
     * 闸门：返回 null 表示放行，非 null 则作为终止响应回给客户端。
     * 仅适用于同步即可判定的检查；按字节累加的限额与成对的配额归还分别落在 reactor 操作符与 {@link #release}。
     */
    @FunctionalInterface
    private interface RequestGate {
        Publisher<Void> check(RequestContext ctx);
    }

    /** 一次请求在闸门链上传递的状态：每个 gate 只写自己负责的字段。 */
    private static final class RequestContext {
        final HttpServerRequest request;
        final HttpServerResponse response;

        String path; // 规范化后的路径
        HandlerManager.HandlerEntry entry;
        HttpRequestImpl httpRequest;
        HttpHandler.StreamHandler streamHandler;
        boolean streamRequired;
        long reservedBytes; // 聚合分支已预留的内存配额

        RequestContext(HttpServerRequest request, HttpServerResponse response) {
            this.request = request;
            this.response = response;
        }
    }
}
