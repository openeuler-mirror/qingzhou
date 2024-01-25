package qingzhou.remote.impl.net.http;

public class HttpServerServiceImpl implements HttpServerService {
    @Override
    public HttpServer createHttpServer(int port, int backlog) throws Exception {
        return new TinyHttpServer(port, backlog);
    }
}
