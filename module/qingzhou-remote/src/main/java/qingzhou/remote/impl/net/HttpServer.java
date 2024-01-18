package qingzhou.remote.impl.net;

public interface HttpServer {
    void addContext(HttpRoute httpRoute, HttpHandler httpHandler);

    void removeContext(String path);

    void start();

    void stop(int delaySeconds);
}
