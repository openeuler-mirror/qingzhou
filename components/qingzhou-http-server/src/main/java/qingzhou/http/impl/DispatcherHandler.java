package qingzhou.http.impl;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import qingzhou.http.server.AuthResult;
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

class DispatcherHandler implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {
    private static final byte[] NULL_BYTES = new byte[0];
    private static final int STREAM_QUEUE_SIZE = 1024; // 慢客户端时最多缓存的响应分片数

    private final HttpServerImpl httpServer;
    private final Logger logger;
    private final Semaphore concurrentSemaphore;

    DispatcherHandler(HttpServerImpl httpServer, Logger logger) {
        this.httpServer = httpServer;
        this.logger = logger;
        this.concurrentSemaphore = new Semaphore(httpServer.maxConcurrentRequests);
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        addSecurityHeaders(response); // 须在任何 return 之前：400/401/404/413/503 等分支同样需要安全头
        if (!concurrentSemaphore.tryAcquire()) { // 请求体按上限聚合进内存，不限在途请求数则内存仍会耗尽
            return close(response, HttpResponseStatus.SERVICE_UNAVAILABLE);
        }
        // 延迟执行：分发逻辑同步抛异常时也能走到 doFinally，不至于泄漏许可
        return Flux.defer(() -> dispatch(request, response)).doFinally(signal -> concurrentSemaphore.release());
    }

    private Publisher<Void> dispatch(HttpServerRequest request, HttpServerResponse response) {
        String requestPath = request.uri().split("\\?")[0];
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

        HttpHandler httpHandler = httpServer.findHandler(requestPath);
        if (httpHandler == null) {
            return reject(request, response, HttpResponseStatus.NOT_FOUND);
        }

        // 安全认证
        boolean needAuth = !httpServer.isAuthDisabled && !httpServer.noAuthHandlerSet.contains(httpHandler);
        if (needAuth) {
            AuthResult authResult = httpServer.authenticate(httpRequest);
            if (authResult.status() != AuthResult.Status.PASS) {
                return reject(request, response
                                .header(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_STORE),
                        HttpResponseStatus.UNAUTHORIZED);
            }
            if (authResult.getPrincipal() != null) {
                httpRequest.setAttribute(AuthResult.AUTH_PRINCIPAL_ATTRIBUTE, authResult.getPrincipal());
            }
            Set<String> roles = authResult.getRoles();
            httpRequest.setAttribute(AuthResult.AUTH_ROLES_ATTRIBUTE, roles == null ? Collections.emptySet()
                    : Collections.unmodifiableSet(new HashSet<>(roles)));
        }

        // 开始处理业务...
        HttpHandler.StreamHandler streamHandler = httpHandler.buildStreamHandler();
        boolean streamRequired = request.method() == HttpMethod.POST && request.isMultipart();
        if (streamRequired && streamHandler == null) {
            return reject(request, response, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        Sinks.Many<byte[]> streamResponse = Sinks.many().unicast()
                .onBackpressureBuffer(Queues.<byte[]>get(STREAM_QUEUE_SIZE).get()); // 有界缓冲：慢客户端不再导致内存无界堆积
        HttpResponseImpl httpResponse = new HttpResponseImpl(response, streamResponse);

        if (streamRequired) {
            if (bodyTooLarge(request, httpServer.maxStreamBytes)) { // 预检：超限直接拒，不必先落临时文件
                return reject(request, response, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
            }
            streamHandler.onBegin(httpRequest, httpResponse);
            AtomicLong receivedBytes = new AtomicLong();
            request.receive() // 下面开始 直接订阅原始数据流，不进行 聚合
                    .doOnNext(byteBuf -> { // 限制上传总量，超限触发 onError，由 handler 清理临时文件并回错误
                        if (receivedBytes.addAndGet(byteBuf.readableBytes()) > httpServer.maxStreamBytes) {
                            throw new BodyTooLargeException();
                        }
                    })
                    .subscribe(byteBuf -> {
                                byte[] bytes = new byte[byteBuf.readableBytes()];
                                byteBuf.readBytes(bytes);
                                streamHandler.onNext(bytes);

                                // Reactor Netty 对 ByteBuf 的生命周期管理遵循「发布者负责释放，订阅者负责引用计数」的原则：
                                //request.receive() 产生的 ByteBuf 由 Reactor Netty 框架管理，框架会在数据处理完成后自动释放；
                                //手动调用 byteBuf.release() 会导致 ByteBuf 的引用计数被提前耗尽，可能引发两种严重问题：
                                //重复释放（Double Release）：框架后续尝试释放已被手动释放的 ByteBuf，触发 IllegalReferenceCountException；
                                // byteBuf.release();
                            },
                            err -> {
                                streamHandler.onError(err);
                                // 兜底：handler 若未发响应就返回，响应链永不结束，请求会一直挂到超时
                                if (!httpResponse.isUsed()) httpResponse.status500Finish(err.getMessage());
                            },
                            streamHandler::onComplete // 完成信号
                    );
            return response.sendByteArray(streamResponse.asFlux()).then();
        } else {
            if (bodyTooLarge(request, httpServer.maxBodyBytes)) {
                return reject(request, response, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
            }
            AtomicLong receivedBytes = new AtomicLong();
            return ByteBufFlux.fromInbound(request.receive()
                            .doOnNext(byteBuf -> { // chunked 请求无 Content-Length，聚合前按实际字节数二次限制
                                if (receivedBytes.addAndGet(byteBuf.readableBytes()) > httpServer.maxBodyBytes) {
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
                            response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                            Throwable cause = getCause(e);
                            logger.error("http handler error", cause);
                            streamResponse.tryEmitError(cause);
                        }
                        return response.sendByteArray(streamResponse.asFlux()).then();
                    })
                    .onErrorResume(e -> close(response, statusOf(e))); // 连接中断等也应回响应，且不能一律报 413
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
                    if (receivedBytes.addAndGet(byteBuf.readableBytes()) > httpServer.maxBodyBytes) {
                        throw new BodyTooLargeException();
                    }
                })
                .then(errorResponse)
                .onErrorResume(e -> close(response, statusOf(e)));
    }

    // 请求体未读完，连接无法复用：关闭它，避免残留字节污染后续请求
    private static Mono<Void> close(HttpServerResponse response, HttpResponseStatus status) {
        return response
                .status(status)
                .header(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                .send();
    }

    /**
     * 路径规范化：折叠重复斜杠与 "." 段；发现 ".." 段返回 null（拒绝请求）。
     * 各 handler 自行解析路径，防护难以统一，故在分发入口一次性拦截穿越。
     */
    private static String normalize(String path) {
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
    private static boolean isDoubleEncoded(String path) {
        String upper = path.toUpperCase(Locale.ROOT);
        return upper.contains("%2E") || upper.contains("%25");
    }

    // Content-Length 预检给出干净的 413；非法头交给字节计数兜底
    private static boolean bodyTooLarge(HttpServerRequest request, long limit) {
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
        if (httpServer.csp != null && !httpServer.csp.isEmpty() && !httpServer.csp.equals("none")) {
            response.header("Content-Security-Policy", httpServer.csp);
        }
        if (httpServer.isSslEnabled) {
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
