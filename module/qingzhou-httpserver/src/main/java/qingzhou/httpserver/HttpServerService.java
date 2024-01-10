package qingzhou.httpserver;

public interface HttpServerService {
    HttpServer createHttpServer(int port, int backlog) throws Exception;
}
