package qingzhou.http.server.impl;

import java.util.List;
import java.util.Map;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import qingzhou.common.Configs;

/**
 * 响应安全头与跨域头。
 */
final class ResponseHeaders {
    private final String csp; // "none" 或空串表示不下发 CSP
    private final String permissionsPolicy;
    private final String coop;
    private final String corp;
    private final List<String> corsAllowedOrigins; // 允许的跨域来源；空表示不开放跨域
    private final boolean hstsPreload;
    private final long hstsMaxAge; // HSTS 有效期：部署形态（含内网/预发布）不同，收敛周期应可配

    ResponseHeaders(Map<String, String> config) {
        Configs configs = new Configs(config);
        // 前端（qingzhou-web/index.html）使用内联脚本，script-src 须保留 'unsafe-inline'；
        // 收紧会造成白屏，可通过 csp 配置项按部署形态覆盖
        this.csp = configs.getOrDefault("csp", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'self'; object-src 'none'; base-uri 'self'");
        this.permissionsPolicy = configs.getOrDefault("permissions_policy", "geolocation=(), microphone=(), camera=()");
        this.coop = configs.getOrDefault("coop", "");
        this.corp = configs.getOrDefault("corp", "");
        this.corsAllowedOrigins = configs.splitList("cors_allowed_origins");
        this.hstsPreload = configs.getOrDefault("hsts_preload", false);
        this.hstsMaxAge = configs.atLeast("hsts_max_age", 0, 31536000);
    }

    // 须在任何 return 之前调用：400/401/404/413/503 等分支同样需要安全头
    void apply(HttpServerRequest request, HttpServerResponse response) {
        response.header("X-Content-Type-Options", "nosniff")
                // 与 CSP 的 frame-ancestors 并存：不支持 CSP 的老浏览器仅认此头，二者不冲突
                .header("X-Frame-Options", "SAMEORIGIN")
                .header("Referrer-Policy", "no-referrer")
                .header("Permissions-Policy", permissionsPolicy)
                // 默认不缓存：认证后的页面与接口数据不得落入代理/浏览器缓存；handler 需要缓存时自行覆盖
                .header(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_STORE);
        if (csp != null && !csp.isEmpty() && !csp.equals("none")) {
            response.header("Content-Security-Policy", csp);
        }
        if (!coop.isEmpty()) response.header("Cross-Origin-Opener-Policy", coop);
        if (!corp.isEmpty()) response.header("Cross-Origin-Resource-Policy", corp);
        boolean isTls = "https".equals(request.scheme());
        if (isTls) response.header("Strict-Transport-Security",
                "max-age=" + hstsMaxAge + "; includeSubDomains" + (hstsPreload ? "; preload" : ""));
        applyCors(request, response);
    }

    /**
     * 跨域头对预检与实际响应同样必要：只在预检回显会让浏览器在真实请求阶段被拦，
     * cors_allowed_origins 形同虚设。白名单为空表示不开放跨域。
     */
    private void applyCors(HttpServerRequest request, HttpServerResponse response) {
        if (corsAllowedOrigins.isEmpty()) return;
        String origin = request.requestHeaders().get(HttpHeaderNames.ORIGIN);
        if (origin == null) return;

        String matched = matchedOrigin(origin);
        if (matched == null) return;

        boolean wildcard = "*".equals(matched);
        response.header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, wildcard ? "*" : origin)
                .header(HttpHeaderNames.VARY, HttpHeaderNames.ORIGIN.toString());
        // 与通配来源并存会被浏览器直接拒绝，故仅在明确列举来源时声明可携带凭据
        if (!wildcard) response.header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    // 返回命中的白名单项："*" 表示放开全部来源，null 表示不在白名单内
    private String matchedOrigin(String origin) {
        for (String allowed : corsAllowedOrigins) {
            if ("*".equals(allowed) || allowed.equals(origin)) return allowed;
        }
        return null;
    }
}
