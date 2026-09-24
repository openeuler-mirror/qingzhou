package qingzhou.http.impl;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.reactivestreams.Publisher;
import qingzhou.http.server.HttpHandler;
import qingzhou.logger.Logger;
import reactor.core.Disposable;
import reactor.core.Exceptions;
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
    private static final int RESPONSE_QUEUE_SIZE = 1024; // 慢客户端时最多缓存的响应分片数
    private static final long UNKNOWN_BODY_SIZE = -1; // chunked：长度未知
    private static final long INVALID_BODY_SIZE = -2; // 非法 Content-Length 或与 Transfer-Encoding 并存
    // 方法白名单与 Allow 头共用同一份数据：两处各自维护必然在增删方法时不同步
    private static final HttpMethod[] ALLOWED_METHODS = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
            HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.HEAD, HttpMethod.OPTIONS};
    private static final String ALLOW_HEADER = Arrays.stream(ALLOWED_METHODS)
            .map(HttpMethod::name)
            .collect(Collectors.joining(", "));

    // 顺序即优先级：读 ctx.path 的须排在 denyUnnormalizedPath 之后，读 ctx.entry 的须排在 denyUnroutedPath 之后
    private final List<RequestGate> gates = Arrays.asList(
            this::denyDisallowedMethod, // 405
            this::denyUnnormalizedPath, // 400：非 origin-form / 解码失败 / 多层编码 / 路径穿越
            this::denyUnroutedPath,     // 404
            this::denyPreflight,   // 204：预检免认证但须已路由，故排在认证之前
            this::denyUnauthenticated,  // 401
            this::denyUnsupportedBody,  // 415
            this::denyOversizedBody,    // 400 / 413
            this::denyExhaustedMemory   // 503
    );

    private final AtomicLong aggregatedBytes = new AtomicLong();

    // 以下由 OSGi 激活线程写入、Netty EventLoop 读取，须保证可见性
    private volatile Semaphore concurrentSemaphore;
    private volatile boolean isSslEnabled;
    private volatile long maxStreamBytes; // 流式上传总量上限
    private volatile int maxBodyBytes; // 单个请求体聚合进内存的上限
    private volatile long maxAggregatedBytes; // 同时在途的聚合请求体总量上限
    private volatile String csp;
    private volatile String[] corsAllowedOrigins; // 允许的跨域来源；空表示不开放 CORS

    @Activate
    public void init(Map<String, String> config) {
        int maxConcurrentRequests = getConfig(config, "max_concurrent_requests", 1000);
        if (maxConcurrentRequests <= 0) throw new IllegalArgumentException("max_concurrent_requests must be positive");
        this.concurrentSemaphore = new Semaphore(maxConcurrentRequests);

        isSslEnabled = getConfig(config, "ssl_enabled", true);

        maxStreamBytes = getConfig(config, "max_stream_bytes", 1024L * 1024 * 1024);
        maxBodyBytes = getConfig(config, "max_body_bytes", 8 * 1024 * 1024);
        maxAggregatedBytes = getConfig(config, "max_aggregated_bytes", 64L * 1024 * 1024);
        if (maxBodyBytes > maxAggregatedBytes) { // 否则长度未知的请求恒被拒，服务不可用却无明确报错
            throw new IllegalArgumentException("max_body_bytes must not exceed max_aggregated_bytes");
        }
        csp = getConfig(config, "csp", "none");
        corsAllowedOrigins = splitOrigins(getConfig(config, "cors_allowed_origins", ""));
    }

    private String[] splitOrigins(String value) {
        if (value.isEmpty()) return new String[0];
        List<String> origins = new ArrayList<>();
        for (String origin : value.split(",")) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) origins.add(trimmed);
        }
        return origins.toArray(new String[0]);
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        addSecurityHeaders(response); // 须在任何 return 之前：400/401/404/413/503 等分支同样需要安全头
        if (!concurrentSemaphore.tryAcquire()) { // 请求体按上限聚合进内存，不限在途请求数则内存仍会耗尽
            return respond(response, HttpResponseStatus.SERVICE_UNAVAILABLE);
        }
        RequestContext ctx = new RequestContext(request, response);
        // 闸门判定同步执行：它有副作用（预留全局配额），放进 defer 会在重复订阅时被重复执行
        Publisher<Void> denial;
        try {
            denial = checkGates(ctx);
        } catch (Throwable e) {
            release(ctx);
            throw e;
        }
        // 延迟执行：业务处理同步抛异常时也能走到 doFinally，不至于泄漏许可与配额
        return Flux.defer(() -> denial != null ? denial : dispatch(ctx)).doFinally(signal -> release(ctx));
    }

    // 许可与内存配额统一在入口归还：gate 只管判定，不必关心释放，也不受 gate 顺序影响
    private void release(RequestContext ctx) {
        if (!ctx.released.compareAndSet(false, true)) return; // 归还次数须与 tryAcquire 一致，重复订阅不得超发许可
        concurrentSemaphore.release();
        if (ctx.reservedBytes > 0) aggregatedBytes.addAndGet(-ctx.reservedBytes);
    }

    // 返回 null 表示放行，非 null 则作为终止响应；顺序即优先级，见 init() 中的 gates
    private Publisher<Void> checkGates(RequestContext ctx) {
        for (RequestGate gate : gates) {
            Publisher<Void> denial = gate.check(ctx);
            if (denial != null) return denial;
        }
        return null;
    }

    private Publisher<Void> dispatch(RequestContext ctx) {
        Sinks.Many<byte[]> streamResponse = Sinks.many().unicast()
                .onBackpressureBuffer(Queues.<byte[]>get(RESPONSE_QUEUE_SIZE).get()); // 有界缓冲：慢客户端不再导致内存无界堆积
        HttpResponseImpl httpResponse = new HttpResponseImpl(ctx.response, streamResponse);
        return ctx.streamRequired ? receiveStream(ctx, httpResponse, streamResponse)
                : aggregate(ctx, httpResponse, streamResponse);
    }

    private Publisher<Void> receiveStream(RequestContext ctx, HttpResponseImpl httpResponse, Sinks.Many<byte[]> streamResponse) {
        HttpHandler.StreamHandler streamHandler = ctx.streamHandler;
        Throwable beginError = safeRun(() -> streamHandler.onBegin(ctx.httpRequest, httpResponse));
        if (beginError != null) {
            logger.error("http handler error", beginError);
            safeRun(() -> streamHandler.onError(beginError)); // onBegin 可能已建临时文件，须给 handler 清理机会
            // handler 回调同步抛出时请求体还没读完，须先排空再回 500，否则残留字节会被当作下一个请求解析
            return respond(ctx.response, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        AtomicLong receivedBytes = new AtomicLong();
        Disposable receiveDisposable = ctx.request.receive() // 下面开始 直接订阅原始数据流，不进行 聚合
                .doOnNext(byteBuf -> { // 限制上传总量，超限触发 onError，由 handler 清理临时文件并回错误
                    if (receivedBytes.addAndGet(byteBuf.readableBytes()) > maxStreamBytes) {
                        throw new BodyTooLargeException();
                    }
                })
                .subscribe(byteBuf -> {
                            byte[] bytes = new byte[byteBuf.readableBytes()];
                            byteBuf.readBytes(bytes);
                            try {
                                streamHandler.onNext(bytes); // 声明 throws Throwable，而 Consumer.accept 不得抛受检异常
                            } catch (Throwable e) {
                                throw Exceptions.propagate(e); // 致命 Error 原样抛出，其余包装后交给下方 err 分支收尾
                            }
                            // ByteBuf 由 Reactor Netty 框架负责释放，无需手动 release，否则引用计数提前耗尽
                        },
                        err -> {
                            Throwable cause = getCause(err);
                            safeRun(() -> streamHandler.onError(cause)); // 回调自身出错不得吞掉下方兜底
                            // 兜底：handler 若未发响应就返回，响应链永不结束，请求会一直挂到超时
                            if (!httpResponse.isCommitted()) {
                                logger.error("http stream handler error", cause); // 仅在真正需要兜底时记录，避免客户端中断也刷日志
                                HttpResponseStatus status = statusOf(cause);
                                httpResponse.status(status.code()).sendFinish(status.reasonPhrase());
                            }
                        },
                        () -> { // 完成信号：回调异常也必须收尾，否则 sink 不结束、许可与配额无法归还
                            Throwable cause = safeRun(streamHandler::onComplete);
                            if (cause != null) logger.error("http stream handler error", cause);
                            httpResponse.finish();
                        }
                );
        return ctx.response.sendByteArray(streamResponse.asFlux())
                .then()
                // 响应结束即停止读请求体：handler 在 onBegin 中就结束响应时，否则会继续读到 maxStreamBytes
                .doFinally(signal -> receiveDisposable.dispose());
    }

    private Publisher<Void> aggregate(RequestContext ctx, HttpResponseImpl httpResponse, Sinks.Many<byte[]> streamResponse) {
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
                        // 幂等：handler 未收尾（含 send 后漏 finish）时补上终止信号，避免请求无限等
                        httpResponse.finish();
                    } catch (Throwable e) {
                        Throwable cause = getCause(e);
                        logger.error("http handler error", cause);
                        if (httpResponse.isCommitted()) {
                            httpResponse.finish(); // 已提交，状态码无法回退，只能干净收尾
                        } else {
                            HttpResponseStatus status = statusOf(cause);
                            httpResponse.status(status.code()).sendFinish(status.reasonPhrase());
                        }
                    }
                    return ctx.response.sendByteArray(streamResponse.asFlux()).then();
                })
                // 连接中断等也应回响应，且不能一律报 413；已写出响应体时不得再发状态行，否则造成响应污染
                .onErrorResume(e -> {
                    if (httpResponse.isCommitted()) {
                        httpResponse.finish(); // 已提交只能补终止信号，否则 sink 不结束、许可无法归还
                        return Mono.empty();
                    }
                    return respond(ctx.response, statusOf(getCause(e)));
                });
    }

    // 405：TRACE 等会落到兜底 handler（注册在 / 的页面），有跨站追踪风险
    private Publisher<Void> denyDisallowedMethod(RequestContext ctx) {
        return isMethodAllowed(ctx.request.method()) ? null
                : respond(ctx.response.header(HttpHeaderNames.ALLOW, ALLOW_HEADER), HttpResponseStatus.METHOD_NOT_ALLOWED);
    }

    // TRACE 等非业务方法不应进入任何 handler；PATCH 为 qingzhou-ai 会话接口所需
    private static boolean isMethodAllowed(HttpMethod method) {
        for (HttpMethod allowed : ALLOWED_METHODS) {
            if (allowed == method) return true;
        }
        return false;
    }

    // 204：仅跨域预检（必带 Access-Control-Request-Method）在此终止，下放业务 handler 会返回页面内容；
    // 预检不携带凭据，故必须免认证；跨域头仅在来源白名单内回显，默认不开放
    private Publisher<Void> denyPreflight(RequestContext ctx) {
        if (ctx.request.method() != HttpMethod.OPTIONS) return null;
        HttpHeaders headers = ctx.request.requestHeaders();
        if (headers.get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD) == null) return null; // 普通探测仍走 handler

        HttpServerResponse response = ctx.response.header(HttpHeaderNames.ALLOW, ALLOW_HEADER);
        String origin = headers.get(HttpHeaderNames.ORIGIN);
        if (origin != null && isOriginAllowed(origin)) {
            String requestedHeaders = headers.get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS);
            response.header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
                    .header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, ALLOW_HEADER)
                    // 回显客户端实际请求的头：固定值会让携带其它自定义头的跨域请求被浏览器拒绝
                    .header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS,
                            requestedHeaders == null || requestedHeaders.isEmpty()
                                    ? "Content-Type, Authorization" : requestedHeaders)
                    .header(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "86400")
                    .header(HttpHeaderNames.VARY, HttpHeaderNames.ORIGIN.toString());
        }
        return respond(response, HttpResponseStatus.NO_CONTENT); // 204 无体，不得带 Content-Length
    }

    // 白名单为空表示不开放跨域；"*" 表示放开全部来源
    private boolean isOriginAllowed(String origin) {
        for (String allowed : corsAllowedOrigins) {
            if ("*".equals(allowed) || allowed.equals(origin)) return true;
        }
        return false;
    }

    // 400：非 origin-form、解码失败、多层编码、含 ".." 段，四者在分发入口一并拦截；
    // 各 handler 自行解析路径，防护难以统一，故不在此放行任何可疑形态
    private Publisher<Void> denyUnnormalizedPath(RequestContext ctx) {
        String uri = ctx.request.uri();
        int queryIndex = uri.indexOf('?');
        String rawPath = queryIndex < 0 ? uri : uri.substring(0, queryIndex);
        // absolute-form（http://host/x）与 "*" 会被下游按 origin-form 解析出错误路径，直接拒绝
        if (!rawPath.startsWith("/")) return respond(ctx, HttpResponseStatus.BAD_REQUEST);

        String path;
        try {
            // 只解码 %XX：URLDecoder 会把路径中的 '+' 解成空格，改写请求语义
            path = QueryStringDecoder.decodeComponent(rawPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return respond(ctx, HttpResponseStatus.BAD_REQUEST);
        }
        if (hasDangerousEncoding(path)) return respond(ctx, HttpResponseStatus.BAD_REQUEST);
        path = normalize(path);
        if (path == null) return respond(ctx, HttpResponseStatus.BAD_REQUEST);

        ctx.path = path;
        ctx.httpRequest = new HttpRequestImpl(ctx.request, path);
        return null;
    }

    /**
     * 路径规范化：折叠重复斜杠与 "." 段；发现 ".." 段返回 null（拒绝请求）。
     * 各 handler 自行解析路径，防护难以统一，故在分发入口一次性拦截穿越。
     */
    private String normalize(String path) {
        if (path.indexOf('\0') >= 0) return null; // NUL 会被下游文件 API 截断，可能绕过路径校验
        // 反斜杠在 Windows 上等价分隔符，矩阵参数会被下游解析截断，二者都可能绕过基于路径的校验
        if (path.indexOf('\\') >= 0 || path.indexOf(';') >= 0) return null;
        StringBuilder normalized = new StringBuilder(path.length());
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || segment.equals(".")) continue;
            if (segment.equals("..")) return null;
            normalized.append('/').append(segment);
        }
        if (path.endsWith("/") && normalized.length() > 0) normalized.append('/');
        return normalized.length() == 0 ? "/" : normalized.toString();
    }

    private Publisher<Void> denyUnroutedPath(RequestContext ctx) {
        ctx.entry = handlerManager.findHandler(ctx.path);
        return ctx.entry == null ? respond(ctx, HttpResponseStatus.NOT_FOUND) : null;
    }

    private Publisher<Void> denyUnauthenticated(RequestContext ctx) {
        if (authManager.doAuth(ctx.httpRequest, ctx.entry)) return null;
        return respond(ctx.response.header(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_STORE),
                HttpResponseStatus.UNAUTHORIZED);
    }

    // 415：multipart 必须有 StreamHandler；handler 自身抛出视为 500
    private Publisher<Void> denyUnsupportedBody(RequestContext ctx) {
        if (ctx.request.method() != HttpMethod.POST || !ctx.request.isMultipart()) return null;
        ctx.streamRequired = true;
        try {
            ctx.streamHandler = ctx.entry.handler.multipartStreamHandler();
        } catch (Throwable e) {
            logger.error("http handler error", getCause(e));
            return respond(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        return ctx.streamHandler == null ? respond(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE) : null;
    }

    // 400：Content-Length 非法或与 Transfer-Encoding 并存；413：声明长度超限。
    // 非法头不能按"长度未知"预留配额，否则少量并发即可耗尽聚合预算
    private Publisher<Void> denyOversizedBody(RequestContext ctx) {
        long size = declaredBodySize(ctx.request);
        if (size == INVALID_BODY_SIZE) return respond(ctx, HttpResponseStatus.BAD_REQUEST);
        if (size > (ctx.streamRequired ? maxStreamBytes : maxBodyBytes)) {
            return respond(ctx, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
        }
        return null;
    }

    // 503：仅聚合分支吃堆内存；流式由 handler 落临时文件，不占此预算
    private Publisher<Void> denyExhaustedMemory(RequestContext ctx) {
        ctx.reservedBytes = ctx.streamRequired ? 0 : reserveAggregation(ctx.request);
        return ctx.reservedBytes < 0 ? respond(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE) : null;
    }

    private long reserveAggregation(HttpServerRequest request) {
        // 返回预留的字节数；-1 表示在途聚合总量已达上限
        long size = declaredBodySize(request);
        if (size == UNKNOWN_BODY_SIZE) size = maxBodyBytes; // 长度未知，按单体上限保守预留
        else if (size < 0) size = 0; // 非法头已由 denyOversizedBody 拒绝，此处不再预留
        while (true) {
            long current = aggregatedBytes.get();
            if (current + size > maxAggregatedBytes) return -1;
            if (aggregatedBytes.compareAndSet(current, current + size)) return size;
        }
    }

    // 一次解码后仍残留 . / \ NUL 的编码形态，说明原始路径被多层编码（如 %252e%252e、%252f），
    // 各 handler 的穿越防护未必覆盖得住；只拦危险编码，避免误拒文件名含 % 的合法路径
    private boolean hasDangerousEncoding(String path) {
        String upper = path.toUpperCase(Locale.ROOT);
        return upper.contains("%2E") || upper.contains("%2F") || upper.contains("%5C") || upper.contains("%00");
    }

    // 无请求体返回 0；chunked 返回 UNKNOWN_BODY_SIZE；
    // 非法 Content-Length 或与 Transfer-Encoding 并存返回 INVALID_BODY_SIZE（请求走私特征，调用方回 400）
    private long declaredBodySize(HttpServerRequest request) {
        boolean chunked = request.requestHeaders()
                .contains(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED, true);
        String value = request.requestHeaders().get(HttpHeaderNames.CONTENT_LENGTH);
        if (value != null) {
            if (chunked) return INVALID_BODY_SIZE;
            try {
                long size = Long.parseLong(value);
                return size < 0 ? INVALID_BODY_SIZE : size; // 负长度会按 0 预留，绕过全局聚合预算
            } catch (NumberFormatException e) {
                return INVALID_BODY_SIZE;
            }
        }
        return chunked ? UNKNOWN_BODY_SIZE : 0;
    }

    private Publisher<Void> respond(RequestContext ctx, HttpResponseStatus status) {
        return respond(ctx.response, status);
    }

    /**
     * 终止响应一律不读请求体并关闭连接：
     * 既避免错误分支被当成免费上传通道，也避免残留请求体在保活连接上被当作下一个请求解析（请求走私）。
     */
    private Mono<Void> respond(HttpServerResponse response, HttpResponseStatus status) {
        return response
                .status(status)
                .header(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                .send();
    }

    // 入参须为最内层 cause：Reactor 会包装异常，调用方先用 getCause 展开再传入，避免重复遍历
    private HttpResponseStatus statusOf(Throwable cause) {
        return cause instanceof BodyTooLargeException
                ? HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE
                : HttpResponseStatus.INTERNAL_SERVER_ERROR;
    }

    private void addSecurityHeaders(HttpServerResponse response) {
        response.header("X-Content-Type-Options", "nosniff")
                .header("Referrer-Policy", "no-referrer")
                .header("Cross-Origin-Opener-Policy", "same-origin")
                .header("Cross-Origin-Resource-Policy", "same-origin")
                .header("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        if (csp != null && !csp.isEmpty() && !csp.equals("none")) {
            response.header("Content-Security-Policy", csp); // 嵌套控制交给 frame-ancestors，不再输出 X-Frame-Options
        } else {
            response.header("X-Frame-Options", "SAMEORIGIN");
        }
        if (isSslEnabled) {
            response.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
    }

    private static Throwable getCause(Throwable e) {
        Throwable cause = e;

        while (cause != null
                && cause.getCause() != null
                && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        return cause;
    }

    // 回调可抛 Throwable（含 OSGi 刷 bundle 时的 NoClassDefFoundError），一律收敛为返回值，避免中断收尾链
    private static Throwable safeRun(Callback callback) {
        try {
            callback.run();
            return null;
        } catch (Throwable e) {
            return getCause(e);
        }
    }

    private interface Callback {
        void run() throws Throwable;
    }

    // 请求体超限的内部信号：仅用于在 Reactor 链上把 413 与 500 区分开，不对外暴露
    private static final class BodyTooLargeException extends RuntimeException {
        BodyTooLargeException() {
            super("request body too large");
        }
    }

    /**
     * 闸门：返回 null 表示放行，非 null 则作为终止响应回给客户端。
     * 仅适用于同步即可判定的检查；按字节累加的限额与成对的配额归还分别落在 reactor 操作符与 release。
     */
    private interface RequestGate {
        Publisher<Void> check(RequestContext ctx);
    }

    /**
     * 一次请求在闸门链上传递的状态：每个 gate 只写自己负责的字段。
     */
    private static final class RequestContext {
        final HttpServerRequest request;
        final HttpServerResponse response;
        final AtomicBoolean released = new AtomicBoolean();

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
