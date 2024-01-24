package qingzhou.remote.impl.net.http;


public interface HttpServerService {
    HttpServer createHttpServer(int port, int backlog) throws Exception;
}
