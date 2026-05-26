package qingzhou.http.server;

public interface HttpServer {
    void registerHttpHandler(HttpHandler httpHandler, String handlePath);
    void unregisterHttpHandler(HttpHandler httpHandler);
}