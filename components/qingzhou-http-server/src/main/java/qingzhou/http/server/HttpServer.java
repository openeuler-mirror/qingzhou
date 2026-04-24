package qingzhou.http.server;

public interface HttpServer {
    void registerHttpHandler(String handlePath, HttpHandler httpHandler);
}
