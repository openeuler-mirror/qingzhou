package qingzhou.app.oauth2;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.api.annotations.App;
import qingzhou.api.annotations.Menu;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpServer;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

@App(icon = "Key", roles = "admin",
        name = {"OAuth2 授权服务", "en:OAuth2 Server"},
        info = {"提供 OAuth 2.0 / OpenID Connect 1.0 授权码、PKCE、ID Token、发现、JWKS、内省与撤销能力。",
                "en:OAuth 2.0 / OpenID Connect 1.0 server with authorization code, PKCE, ID Token, discovery, JWKS, introspection and revocation."})
@Menu(name = {"接入管理", "en:Access Management"}, code = "access", icon = "Grid", order = 1)
public class OAuth2App implements QingzhouApp {
    private static final String PATH_PREFIX = "/oauth2-server";
    private static final String[] ENDPOINT_PATHS = {
            "/authorize", "/token", "/userinfo", "/introspect", "/revoke", "/guide", "/jwks.json",
            "/.well-known/oauth-authorization-server", "/.well-known/openid-configuration"
    };

    private HttpServer httpServer;
    private HttpHandler[] endpoints;
    private ScheduledExecutorService cleanupExecutor;

    @Override
    public void start(AppContext appContext) throws Exception {
        Logger logger = appContext.getService(Logger.class);
        Json json = appContext.getService(Json.class);
        if (json == null) {
            if (logger != null) logger.error("Json 不可用，oauth2 端点未注册");
            return;
        }

        Store store;
        try {
            store = Store.get(appContext);
        } catch (Throwable e) {
            if (logger != null) logger.error("oauth2 存储初始化失败，端点未注册", e);
            return;
        }

        Map<String, String> config = appContext.getAppConfig();
        boolean implicitEnabled = Boolean.parseBoolean(config.getOrDefault("implicit_enabled", "false"));
        boolean pkceRequired = Boolean.parseBoolean(config.getOrDefault("pkce_required", "false"));
        boolean revokeAllowAnonymous = Boolean.parseBoolean(config.getOrDefault("revoke_allow_anonymous", "true"));
        String scopesSupported = config.getOrDefault("scopes_supported", "read,write,openid,profile,email");
        Throttle throttle = new Throttle();
        Oidc oidc = new Oidc(json, config);
        endpoints = new HttpHandler[]{
                new Authorize(store, json, oidc, throttle, implicitEnabled, pkceRequired),
                new Token(store, json, oidc, throttle, pkceRequired),
                new Userinfo(store, json),
                new Introspect(store, json),
                new Revoke(store, json, revokeAllowAnonymous),
                new GuidePage(store),
                new Jwks(oidc, json),
                new Discovery(store, json, oidc, implicitEnabled, scopesSupported, false),
                new Discovery(store, json, oidc, implicitEnabled, scopesSupported, true)
        };

        try {
            httpServer = appContext.getService(HttpServer.class);
        } catch (Exception e) {
            if (logger != null) logger.error("HttpServer 不可用，oauth2 端点未注册", e);
            return;
        }
        if (httpServer == null) {
            if (logger != null) logger.error("HttpServer 不可用，oauth2 端点未注册");
            return;
        }

        for (int i = 0; i < endpoints.length; i++) {
            // 授权登录页可能运行在 sandboxed iframe/WebView 中，浏览器会发送 Origin: null；
            // 该端点不依赖 Cookie 会话，显式允许；其他端点仍由平台 CSRF 闸门拒绝。
            boolean allowOpaqueOrigin = "/authorize".equals(ENDPOINT_PATHS[i]);
            httpServer.registerHttpHandler(endpoints[i], PATH_PREFIX + ENDPOINT_PATHS[i], true, allowOpaqueOrigin);
        }

        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "oauth2-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        cleanupExecutor.scheduleWithFixedDelay(() -> {
            try {
                store.cleanup();
            } catch (Throwable e) {
                if (logger != null) logger.warn("oauth2 过期数据清理失败", e);
            }
        }, 5, 5, TimeUnit.MINUTES);

        if (logger != null) {
            logger.info("oauth2 授权服务已就绪：" + PATH_PREFIX
                    + "（storage=" + store.storageMode() + ", implicit=" + implicitEnabled
                    + ", pkce_required=" + pkceRequired + ", revoke_anonymous=" + revokeAllowAnonymous
                    + ", oidc=true）");
        }
    }

    @Override
    public void stop() {
        if (cleanupExecutor != null) cleanupExecutor.shutdownNow();
        if (httpServer == null) return;
        for (HttpHandler endpoint : endpoints) {
            httpServer.unregisterHttpHandler(endpoint);
        }
    }
}
