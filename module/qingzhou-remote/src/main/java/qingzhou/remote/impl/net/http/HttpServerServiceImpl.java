package qingzhou.remote.impl.net.http;

public class HttpServerServiceImpl implements HttpServerService {
    @Override
    public HttpServer createHttpServer(String host, int port, int backlog) {
        return new TinyHttpServer(port, backlog);
    }
}
