package qingzhou.remote.impl.net;

public interface HttpServerService {
    HttpServer createHttpServer(int port, int backlog) throws Exception;
}
