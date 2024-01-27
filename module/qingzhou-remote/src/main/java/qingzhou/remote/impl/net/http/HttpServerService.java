package qingzhou.remote.impl.net.http;


public interface HttpServerService {
    HttpServer createHttpServer(String host, int port, int backlog) throws Exception;
}
