package qingzhou.http.server.impl;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.*;
import org.reactivestreams.Publisher;
import qingzhou.common.Configs;
import qingzhou.http.server.auth.impl.AuthManager;
import qingzhou.logger.Logger;
import reactor.netty.http.server.HttpServerRequest;

import static qingzhou.http.server.impl.Errors.getCause;

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
    private static final int MAX_DECODE_PASSES = 4; // 路径解码层数上限：超出后交由下方危险编码与规范化兜底

    private final AuthManager authManager;
    private final HandlerManager handlerManager;
    private final Logger logger;
    private final SourceLimiter sourceLimiter; // 逐来源并发限制：0 并发配置表示不启用
    // 全局聚合配额计数：由主类持有并跨配置更新共享，热更新重建本类时在途预留仍归还到同一计数
    private final AtomicLong aggregatedBytes;
    private final long maxStreamBytes; // 流式上传总量上限
    private final long maxBodyBytes; // 单个请求体聚合进内存的上限
    private final long maxAggregatedBytes; // 同时在途的聚合请求体总量上限
    private final Set<String> corsAllowedOrigins; // 显式允许的跨域来源，跨站写请求据此放行
    private final List<RequestGate> gates;

    RequestGates(Map<String, String> config, long maxBodyBytes, long maxStreamBytes, SourceLimiter sourceLimiter, AuthManager authManager, HandlerManager handlerManager,
                 Logger logger, AtomicLong aggregatedBytes) {
        Configs configs = new Configs(config);
        this.authManager = authManager;
        this.handlerManager = handlerManager;
        this.logger = logger;
        this.aggregatedBytes = aggregatedBytes;
        this.sourceLimiter = sourceLimiter;
        this.maxStreamBytes = maxStreamBytes;
        this.maxBodyBytes = maxBodyBytes;
        this.maxAggregatedBytes = configs.atLeast("max_aggregated_bytes", 1L, 64L * 1024 * 1024);
        this.corsAllowedOrigins = new HashSet<>(configs.splitList("cors_allowed_origins"));
        // 否则长度未知的请求恒被拒，服务不可用却无明确报错
        if (this.maxBodyBytes > this.maxAggregatedBytes) {
            throw new IllegalArgumentException("qingzhou-http-server.max_body_bytes must not exceed max_aggregated_bytes");
        }
        // 顺序即优先级：读 ctx.path 的须排在 denyUnnormalizedPath 之后，读 ctx.entry 的须排在 denyUnroutedPath 之后
        this.gates = Arrays.asList(
                this::denyDisallowedMethod, // 405
                this::denyUnnormalizedPath, // 400：非 origin-form / 解码失败 / 多层编码 / 路径穿越
                this::denyUnroutedPath,     // 404
                this::denyPreflight,        // 204：预检免认证但须已路由；须排在方法白名单之前，否则 POST-only 路由收不到预检
                this::denyMethodNotAllowedByHandler, // 405：路由声明的方法白名单，认证前即拒绝
                this::denySourceExhausted,  // 503：单来源在途请求上限；须在认证前拦截，否则未认证洪水不受限即可堆满全局并发额度
                this::denyUnauthenticated,  // 401
                this::denyCrossSiteWrite,   // 403：Cookie 凭据会随跨站请求自动附加，SameSite 挡不住同站跨源
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
    private boolean isMethodAllowed(HttpMethod method) {
        for (HttpMethod allowed : ALLOWED_METHODS) {
            if (allowed == method) return true;
        }
        return false;
    }

    // 供路由注册期校验 HANDLE_METHODS：与全站方法白名单共用同一份数据
    static boolean isSupportedMethod(String method) {
        for (HttpMethod allowed : ALLOWED_METHODS) {
            if (allowed.name().equals(method)) return true;
        }
        return false;
    }

    // 403：状态变更请求的来源必须与本站同源；否则同站跨源表单可借自动附加的 Cookie 执行写操作。
    // 无 Origin 的为非浏览器/服务端调用，不受 CSRF 面影响；跨域 API 须显式列入 cors_allowed_origins
    private Publisher<Void> denyCrossSiteWrite(RequestContext ctx) {
        HttpMethod method = ctx.request.method();
        if (method == HttpMethod.GET || method == HttpMethod.HEAD || method == HttpMethod.OPTIONS) return null;

        String origin = ctx.request.requestHeaders().get(HttpHeaderNames.ORIGIN);
        // 仅「无 Origin 头」视为非浏览器/服务端调用而放行；字面量 "null"（sandboxed iframe、data: 等不透明源）
        // 必须按跨站源校验，否则攻击者可借此绕过同源 CSRF 防线
        if (origin == null) return null;
        String host = ctx.request.requestHeaders().get(HttpHeaderNames.HOST);
        if (host != null && !host.isEmpty() && isSameOrigin(origin, host, ctx.request.scheme())) return null;
        // 跨站写仅认精确列举的来源：白名单含 "*" 时通配语义只作用于 ResponseHeaders 的 CORS 读响应，
        // 不在此放行——否则通配 CORS 与 CSRF 防线叠加，Cookie 认证下同站跨源可借自动附加的 Cookie 完成写操作
        if (corsAllowedOrigins.contains(origin)) return null;
        return ctx.respond(HttpResponseStatus.FORBIDDEN);
    }

    // Origin = scheme://authority[/path]：取 authority 与 Host 精确比较（忽略大小写），
    // 后缀匹配（endsWith("//" + host)）会被 origin=x.evil.com//host 之类的构造绕过。
    // scheme 默认端口在两侧对称剥离：Origin 显式携带而 Host 省略（或反之）不误判跨源
    private static boolean isSameOrigin(String origin, String host, String connectionScheme) {
        int schemeEnd = origin.indexOf("://");
        // 畸形 Origin 缺 scheme 无法构成合法同源，按非同源拒绝，避免被当作裸 authority 放行
        if (schemeEnd < 0) return false;
        String scheme = origin.substring(0, schemeEnd).toLowerCase(Locale.ROOT);
        // 源三要素含 scheme：连接为 https 时 Origin 必须同为 https，否则 http 明文镜像页面可借同 host
        // 发跨 scheme 写请求绕过 CSRF；连接为 http（可能经 TLS 反代，Origin=https 属合法）时不做 scheme 比较
        if ("https".equals(connectionScheme) && !"https".equals(scheme)) return false;
        String authority = origin.substring(schemeEnd + 3);
        int pathStart = authority.indexOf('/');
        if (pathStart >= 0) authority = authority.substring(0, pathStart);
        if ("http".equals(scheme)) {
            authority = stripDefaultPort(authority, ":80");
            host = stripDefaultPort(host, ":80");
        } else if ("https".equals(scheme)) {
            authority = stripDefaultPort(authority, ":443");
            host = stripDefaultPort(host, ":443");
        }
        return host.equalsIgnoreCase(authority);
    }

    private static String stripDefaultPort(String value, String defaultPortSuffix) {
        return value != null && value.toLowerCase(Locale.ROOT).endsWith(defaultPortSuffix)
                ? value.substring(0, value.length() - defaultPortSuffix.length()) : value;
    }

    // 405：路由以 HANDLE_METHODS 声明了允许的方法，不在其中的请求在认证前即拒绝，handler 无需各自判断
    private Publisher<Void> denyMethodNotAllowedByHandler(RequestContext ctx) {
        List<String> methods = ctx.entry.methods;
        if (methods.isEmpty() || methods.contains(ctx.request.method().name())) return null;
        ctx.response.header(HttpHeaderNames.ALLOW, String.join(", ", methods));
        return ctx.respond(HttpResponseStatus.METHOD_NOT_ALLOWED);
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
        // 路由声明了方法白名单时，预检的目标方法也必须在其内，否则「预检 204、真实请求 405」
        List<String> routeMethods = ctx.entry.methods;
        if (!routeMethods.isEmpty() && !routeMethods.contains(requestedMethod)) {
            ctx.response.header(HttpHeaderNames.ALLOW, String.join(", ", routeMethods));
            return ctx.respond(HttpResponseStatus.METHOD_NOT_ALLOWED);
        }

        // 路由声明了方法白名单时，回显它而不是全站白名单：否则浏览器认为 POST 之外的后续请求也可用
        String allowedMethods = routeMethods.isEmpty() ? ALLOW_HEADER : String.join(", ", routeMethods);
        String requestedHeaders = sanitizeRequestedHeaders(headers.get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS));
        ctx.response.header(HttpHeaderNames.ALLOW, allowedMethods)
                .header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, allowedMethods)
                // 仅回显服务端允许的请求头，避免任意自定义头被跨域放行
                .header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, requestedHeaders)
                .header(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "86400");
        return ctx.respond(HttpResponseStatus.NO_CONTENT); // 204 无体，不得带 Content-Length
    }

    // 预检请求头仅允许 Content-Type 与 Authorization；空列表回退默认集
    private static String sanitizeRequestedHeaders(String requestedHeaders) {
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
            // 递归解码至稳定：仅解一层时 %252e 等三层编码仍以编码形态进入 ctx.path，
            // 下游 handler 若二次解码即还原为穿越段；首层解码失败按非法 URI 拒绝
            path = decodeUntilStable(rawPath);
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
     * 路径解码：反复解码直到结果稳定，从机制上消除「下游二次解码才现形」的多层编码穿越
     * （%252e -> %2e -> .）。无法继续解码（如文件名经一次解码后以裸 % 结尾）时保持现值，
     * 单独 % 无危害；达到层数上限的残留交由 hasDangerousEncoding 与 normalize 兜底。
     */
    private static String decodeUntilStable(String rawPath) {
        String current = QueryStringDecoder.decodeComponent(rawPath, StandardCharsets.UTF_8);
        for (int pass = 1; pass < MAX_DECODE_PASSES; pass++) {
            String decoded;
            try {
                decoded = QueryStringDecoder.decodeComponent(current, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return current;
            }
            if (decoded.equals(current)) return current;
            current = decoded;
        }
        return current;
    }

    /**
     * 路径规范化：折叠重复斜杠与 "." 段；发现 ".." 段返回 null（拒绝请求）。
     * 各 handler 自行解析路径，防护难以统一，故在分发入口一次性拦截穿越。
     */
    private static String normalize(String path) {
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

    // 超过解码层数上限仍残留 . / \ NUL 编码形态（4 层以上多层编码攻击），
    // 各 handler 的穿越防护未必覆盖得住；只拦危险编码，避免误拒文件名含 % 的合法路径
    private static boolean hasDangerousEncoding(String path) {
        String upper = path.toUpperCase(Locale.ROOT);
        return upper.contains("%2E") || upper.contains("%2F") || upper.contains("%5C") || upper.contains("%00");
    }

    private Publisher<Void> denyUnroutedPath(RequestContext ctx) {
        ctx.entry = handlerManager.findHandler(ctx.path);
        return ctx.entry == null ? ctx.respond(HttpResponseStatus.NOT_FOUND) : null;
    }

    private Publisher<Void> denyUnauthenticated(RequestContext ctx) {
        // 401 不附带 WWW-Authenticate 挑战头：平台默认走 Cookie/Bearer 会话认证，
        // 无条件的 Basic 挑战会让浏览器在 SPA 场景反复弹原生登录框，破坏前端交互
        return authManager.doAuth(ctx.httpRequest, ctx.httpResponse, ctx.entry) ? null
                : ctx.respond(HttpResponseStatus.UNAUTHORIZED); // no-store 已由安全头统一设置
    }

    // 415：multipart 必须有 StreamHandler；handler 自身抛出视为 500。
    // 不限方法：PUT/PATCH 的 multipart 若退化为聚合路径，会把整个上传体读进内存且 handler 无法解析
    private Publisher<Void> denyUnsupportedBody(RequestContext ctx) {
        if (!ctx.request.isMultipart()) return null;
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

    // 503：单来源在途请求上限。全局配额无来源维度，单 IP 的慢速/并发请求即可耗尽之；
    // 取得的许可记入 ctx，由 DispatcherHandler.release 统一归还，与信号量同思路
    private Publisher<Void> denySourceExhausted(RequestContext ctx) {
        if (!sourceLimiter.isEnabled()) return null;
        String host = ctx.httpRequest.getRemoteHost();
        if (sourceLimiter.tryAcquire(host)) {
            ctx.sourceReservation = new RequestContext.SourceReservation(sourceLimiter, host);
            return null;
        }
        return ctx.respond(HttpResponseStatus.SERVICE_UNAVAILABLE);
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
    private static long declaredBodySize(HttpServerRequest request) {
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
