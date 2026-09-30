package qingzhou.http.server.impl;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.*;
import org.reactivestreams.Publisher;
import qingzhou.http.server.auth.impl.AuthManager;
import qingzhou.logger.Logger;
import reactor.netty.http.server.HttpServerRequest;

import static qingzhou.http.server.util.Configs.atLeast;
import static qingzhou.http.server.util.Errors.getCause;

/**
 * 请求准入闸门链：方法白名单、路径规范化、路由、认证、body 形态与配额。
 * 顺序即优先级；每个 gate 只做同步判定，按字节累加的限额与成对的配额归还
 * 分别落在 reactor 操作符与 DispatcherHandler#release。
 */
final class RequestGates {
    // 方法白名单与 Allow 头共用同一份数据：两处各自维护必然在增删方法时不同步
    private static final HttpMethod[] ALLOWED_METHODS = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
            HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.HEAD, HttpMethod.OPTIONS};
    private static final String ALLOW_HEADER = Arrays.stream(ALLOWED_METHODS)
            .map(HttpMethod::name)
            .collect(Collectors.joining(", "));
    private static final long UNKNOWN_BODY_SIZE = -1; // chunked：长度未知
    private static final long INVALID_BODY_SIZE = -2; // 非法 Content-Length 或与 Transfer-Encoding 并存

    private final AuthManager authManager;
    private final HandlerManager handlerManager;
    private final Logger logger;
    // 全局聚合配额计数：由主类持有并跨配置更新共享，热更新重建本类时在途预留仍归还到同一计数
    private final AtomicLong aggregatedBytes;
    private final long maxStreamBytes; // 流式上传总量上限
    private final long maxBodyBytes; // 单个请求体聚合进内存的上限
    private final long maxAggregatedBytes; // 同时在途的聚合请求体总量上限
    private final List<RequestGate> gates;

    RequestGates(Map<String, String> config, long maxBodyBytes, long maxStreamBytes, AuthManager authManager, HandlerManager handlerManager,
                 Logger logger, AtomicLong aggregatedBytes) {
        this.authManager = authManager;
        this.handlerManager = handlerManager;
        this.logger = logger;
        this.aggregatedBytes = aggregatedBytes;
        this.maxStreamBytes = maxStreamBytes;
        this.maxBodyBytes = maxBodyBytes;
        this.maxAggregatedBytes = atLeast(config, "max_aggregated_bytes", 1L, 64L * 1024 * 1024);
        // 否则长度未知的请求恒被拒，服务不可用却无明确报错
        if (this.maxBodyBytes > this.maxAggregatedBytes) {
            throw new IllegalArgumentException("qingzhou-http-server.max_body_bytes must not exceed max_aggregated_bytes");
        }
        // 顺序即优先级：读 ctx.path 的须排在 denyUnnormalizedPath 之后，读 ctx.entry 的须排在 denyUnroutedPath 之后
        this.gates = Arrays.asList(
                this::denyDisallowedMethod, // 405
                this::denyUnnormalizedPath, // 400：非 origin-form / 解码失败 / 多层编码 / 路径穿越
                this::denyUnroutedPath,     // 404
                this::denyPreflight,        // 204：预检免认证但须已路由，故排在认证之前
                this::denyUnauthenticated,  // 401
                this::denyUnsupportedBody,  // 415
                this::denyOversizedBody,    // 400 / 413
                this::denyExhaustedStreams, // 503：流式上传的并发数上限
                this::denyExhaustedMemory   // 503
        );
    }

    // 返回 null 表示放行，非 null 则作为终止响应；顺序即优先级，见构造器中的 gates
    Publisher<Void> check(RequestContext ctx) {
        for (RequestGate gate : gates) {
            Publisher<Void> denial = gate.check(ctx);
            if (denial != null) return denial;
        }
        return null;
    }

    // 405：TRACE 等会落到兜底 handler（注册在 / 的页面），有跨站追踪风险
    private Publisher<Void> denyDisallowedMethod(RequestContext ctx) {
        if (isMethodAllowed(ctx.request.method())) return null;
        ctx.response.header(HttpHeaderNames.ALLOW, ALLOW_HEADER);
        return ctx.respond(HttpResponseStatus.METHOD_NOT_ALLOWED);
    }

    // TRACE 等非业务方法不应进入任何 handler；PATCH 为 qingzhou-ai 会话接口所需
    private static boolean isMethodAllowed(HttpMethod method) {
        for (HttpMethod allowed : ALLOWED_METHODS) {
            if (allowed == method) return true;
        }
        return false;
    }

    // 204：仅跨域预检（必带 Access-Control-Request-Method）在此终止，下放业务 handler 会返回页面内容；
    // 预检不携带凭据，故必须免认证；跨域头已由入口安全头统一写入，此处只补预检专属头
    private Publisher<Void> denyPreflight(RequestContext ctx) {
        if (ctx.request.method() != HttpMethod.OPTIONS) return null;
        HttpHeaders headers = ctx.request.requestHeaders();
        String requestedMethod = headers.get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD);
        if (requestedMethod == null) return null; // 普通探测仍走 handler
        if (Arrays.stream(ALLOWED_METHODS).noneMatch(method -> method.name().equals(requestedMethod))) {
            return ctx.respond(HttpResponseStatus.METHOD_NOT_ALLOWED);
        }

        String requestedHeaders = sanitizeRequestedHeaders(headers.get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS));
        ctx.response.header(HttpHeaderNames.ALLOW, ALLOW_HEADER)
                .header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, ALLOW_HEADER)
                // 仅回显服务端允许的请求头，避免任意自定义头被跨域放行
                .header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, requestedHeaders)
                .header(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "86400");
        return ctx.respond(HttpResponseStatus.NO_CONTENT); // 204 无体，不得带 Content-Length
    }

    // 预检请求头仅允许 Content-Type 与 Authorization；空列表回退默认集
    private String sanitizeRequestedHeaders(String requestedHeaders) {
        String defaultHeader = "Content-Type, Authorization";
        if (requestedHeaders == null || requestedHeaders.isEmpty()) {
            return defaultHeader;
        }
        List<String> allowed = new ArrayList<>();
        for (String header : requestedHeaders.split(",")) {
            String name = header.trim();
            if ("Content-Type".equalsIgnoreCase(name) || "Authorization".equalsIgnoreCase(name)) {
                allowed.add(name);
            }
        }
        return allowed.isEmpty() ? defaultHeader : String.join(", ", allowed);
    }

    // 400：非 origin-form、解码失败、多层编码、含 ".." 段，四者在分发入口一并拦截；
    // 各 handler 自行解析路径，防护难以统一，故不在此放行任何可疑形态
    private Publisher<Void> denyUnnormalizedPath(RequestContext ctx) {
        String uri = ctx.request.uri();
        int queryIndex = uri.indexOf('?');
        String rawPath = queryIndex < 0 ? uri : uri.substring(0, queryIndex);
        // absolute-form（http://host/x）与 "*" 会被下游按 origin-form 解析出错误路径，直接拒绝
        if (!rawPath.startsWith("/")) return ctx.respond(HttpResponseStatus.BAD_REQUEST);

        String path;
        try {
            // 只解码 %XX：URLDecoder 会把路径中的 '+' 解成空格，改写请求语义
            path = QueryStringDecoder.decodeComponent(rawPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ctx.respond(HttpResponseStatus.BAD_REQUEST);
        }
        if (hasDangerousEncoding(path)) return ctx.respond(HttpResponseStatus.BAD_REQUEST);
        path = normalize(path);
        if (path == null) return ctx.respond(HttpResponseStatus.BAD_REQUEST);

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

    // 一次解码后仍残留 . / \ NUL 的编码形态，说明原始路径被多层编码（如 %252e%252e、%252f），
    // 各 handler 的穿越防护未必覆盖得住；只拦危险编码，避免误拒文件名含 % 的合法路径
    private boolean hasDangerousEncoding(String path) {
        String upper = path.toUpperCase(Locale.ROOT);
        return upper.contains("%2E") || upper.contains("%2F") || upper.contains("%5C") || upper.contains("%00");
    }

    private Publisher<Void> denyUnroutedPath(RequestContext ctx) {
        ctx.entry = handlerManager.findHandler(ctx.path);
        return ctx.entry == null ? ctx.respond(HttpResponseStatus.NOT_FOUND) : null;
    }

    private Publisher<Void> denyUnauthenticated(RequestContext ctx) {
        return authManager.doAuth(ctx.httpRequest, ctx.entry) ? null
                : ctx.respond(HttpResponseStatus.UNAUTHORIZED); // no-store 已由安全头统一设置
    }

    // 415：multipart 必须有 StreamHandler；handler 自身抛出视为 500
    private Publisher<Void> denyUnsupportedBody(RequestContext ctx) {
        if (ctx.request.method() != HttpMethod.POST || !ctx.request.isMultipart()) return null;
        ctx.streamRequired = true;
        try {
            ctx.streamHandler = ctx.entry.handler.multipartStreamHandler();
        } catch (Throwable e) {
            logger.error("http handler error", getCause(e));
            return ctx.respond(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        return ctx.streamHandler == null ? ctx.respond(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE) : null;
    }

    // 400：Content-Length 非法或与 Transfer-Encoding 并存；413：声明长度超限。
    // 非法头不能按"长度未知"预留配额，否则少量并发即可耗尽聚合预算
    private Publisher<Void> denyOversizedBody(RequestContext ctx) {
        long size = declaredBodySize(ctx.request);
        if (size == INVALID_BODY_SIZE) return ctx.respond(HttpResponseStatus.BAD_REQUEST);
        if (size > (ctx.streamRequired ? maxStreamBytes : maxBodyBytes)) {
            return ctx.respond(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
        }
        return null;
    }

    // 503：流式上传不占堆内存预算，但并发数不受限会同时打开大量临时文件与连接
    private Publisher<Void> denyExhaustedStreams(RequestContext ctx) {
        if (!ctx.streamRequired) return null;
        if (!ctx.streamSemaphore.tryAcquire()) return ctx.respond(HttpResponseStatus.SERVICE_UNAVAILABLE);
        ctx.streamPermitHeld = true;
        return null;
    }

    // 503：仅聚合分支吃堆内存；流式由 handler 落临时文件，不占此预算
    private Publisher<Void> denyExhaustedMemory(RequestContext ctx) {
        ctx.reservedBytes = ctx.streamRequired ? 0 : reserveAggregation(ctx.request);
        return ctx.reservedBytes < 0 ? ctx.respond(HttpResponseStatus.SERVICE_UNAVAILABLE) : null;
    }

    // 返回预留的字节数；-1 表示在途聚合总量已达上限
    private long reserveAggregation(HttpServerRequest request) {
        long size = declaredBodySize(request);
        if (size == UNKNOWN_BODY_SIZE) size = maxBodyBytes; // 长度未知，按单体上限保守预留
        else if (size < 0) size = 0; // 非法头已由 denyOversizedBody 拒绝，此处不再预留
        while (true) {
            long current = aggregatedBytes.get();
            if (current + size > maxAggregatedBytes) return -1;
            if (aggregatedBytes.compareAndSet(current, current + size)) return size;
        }
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

    /**
     * 闸门：返回 null 表示放行，非 null 则作为终止响应回给客户端。
     */
    private interface RequestGate {
        Publisher<Void> check(RequestContext ctx);
    }
}
