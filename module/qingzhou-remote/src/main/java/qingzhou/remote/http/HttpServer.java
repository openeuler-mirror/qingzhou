package qingzhou.remote.http;

public interface HttpServer {
    void start(String host, int port, int backlog) throws Exception;

    void stop(int delaySeconds);

    HttpContext createContext(String contextPath);

    void removeContext(String contextPath);
}
