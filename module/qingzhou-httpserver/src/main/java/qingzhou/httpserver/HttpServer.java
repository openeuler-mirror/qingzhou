package qingzhou.httpserver;

public interface HttpServer {
    void addContext(String path, HttpHandler httpHandler);

    void removeContext(String path);

    void start();

    void stop(int delaySeconds);
}
