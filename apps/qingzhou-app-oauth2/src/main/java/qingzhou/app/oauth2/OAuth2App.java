package qingzhou.app.oauth2;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.Menu;
import qingzhou.api.QingzhouApp;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpServer;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

@App(icon = "Key",
        name = {"OAuth2 授权服务", "en:OAuth2 Server"},
        info = {"提供 OAuth 2.0 授权码、密码、客户端凭证与刷新令牌能力。",
                "en:OAuth 2.0 authorization code, password, client credentials and refresh token grants."})
@Menu(name = {"接入管理", "en:Access Management"}, code = "access", icon = "Grid", order = 1)
public class OAuth2App implements QingzhouApp {
    private static final String PATH_PREFIX = "/oauth2";
    private static final String[] ENDPOINT_PATHS = {"/authorize", "/token", "/userinfo", "/introspect", "/revoke"};

    private HttpServer httpServer;
    private HttpHandler[] endpoints;

    @Override
    public void start(AppContext appContext) throws Exception {
        Logger logger = appContext.getService(Logger.class);

        Store store;
        try {
            store = Store.get(appContext);
            if (store == null) return;
        } catch (Throwable e) { // 数据源不可用时不应拖垮整个应用（含 JdbcPool 类缺失的 LinkageError），管控台的模型仍要可用
            logger.error("oauth2 数据源不可用", e);
            return;
        }

        boolean implicitEnabled = Boolean.parseBoolean(
                appContext.getProperties().getProperty("implicit_enabled", "false"));
        Json json = appContext.getService(Json.class);
        endpoints = new HttpHandler[]{
                new Authorize(store, json, implicitEnabled),
                new Token(store, json),
                new Userinfo(store, json),
                new Introspect(store, json),
                new Revoke(store, json)
        };

        try {
            httpServer = appContext.getService(HttpServer.class);
        } catch (Exception e) { // HTTP 服务不可用时不应拖垮整个应用，管控台的模型仍要可用
            logger.error("HttpServer 不可用，oauth2 端点未注册", e);
            return;
        }

        for (int i = 0; i < endpoints.length; i++) {
            httpServer.registerHttpHandlerNoAuth(endpoints[i], PATH_PREFIX + ENDPOINT_PATHS[i]);
        }
        logger.info("oauth2 授权服务已就绪：" + PATH_PREFIX + "（implicit_enabled=" + implicitEnabled + "）");
    }

    @Override
    public void stop() {
        if (httpServer == null) return;
        for (HttpHandler endpoint : endpoints) {
            httpServer.unregisterHttpHandler(endpoint);
        }
    }
}
