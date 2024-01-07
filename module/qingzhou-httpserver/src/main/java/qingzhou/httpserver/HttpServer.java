package qingzhou.httpserver;

public interface HttpServer {
    void addContext(String path, HttpHandler handler);

    void removeContext(String path);

    void start();

    void stop();
}
