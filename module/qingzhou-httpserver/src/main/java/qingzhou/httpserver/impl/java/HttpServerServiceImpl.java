package qingzhou.httpserver.impl.java;

import qingzhou.httpserver.HttpServer;
import qingzhou.httpserver.HttpServerService;

import java.net.InetSocketAddress;

public class HttpServerServiceImpl implements HttpServerService {
    @Override
    public HttpServer createHttpServer(int port, int backlog) throws Exception {
        return new HttpServerImpl(com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), backlog));
    }
}
