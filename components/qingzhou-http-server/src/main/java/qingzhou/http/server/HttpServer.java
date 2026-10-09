package qingzhou.http.server;

public interface HttpServer {
    void registerHttpHandler(HttpHandler httpHandler, String handlePath);

    /**
     * 注册并限定允许的 HTTP 方法；不传 methods 表示允许全部方法。
     */
    void registerHttpHandler(HttpHandler httpHandler, String handlePath, boolean noAuth, String... methods);

    void unregisterHttpHandler(HttpHandler httpHandler);
}
