package qingzhou.remote.impl.net.impl.tinyserver;

import qingzhou.remote.impl.net.HttpServer;
import qingzhou.remote.impl.net.HttpServerService;

public class HttpServerServiceImpl implements HttpServerService {
    @Override
    public HttpServer createHttpServer(int port, int backlog) throws Exception {
        return new TinyHttpServer(port, backlog);
    }
}
