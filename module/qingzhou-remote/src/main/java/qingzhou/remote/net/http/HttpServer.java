package qingzhou.remote.net.http;

public interface HttpServer {

    void addContext(HttpRoute httpRoute, HttpHandler httpHandler);

    void removeContext(String path);

    void start();

    void stop(int delaySeconds);
}
