package qingzhou.remote.net.http;


public interface HttpServerService {
    HttpServer createHttpServer(String host, int port, int backlog) throws Exception;
}
