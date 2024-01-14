package qingzhou.httpserver.impl;

import qingzhou.framework.ServiceRegister;
import qingzhou.httpserver.HttpServerService;
import qingzhou.httpserver.impl.tinyserver.TinyHttpServer;

public class Controller extends ServiceRegister<HttpServerService> {
    @Override
    protected Class<HttpServerService> serviceType() {
        return HttpServerService.class;
    }

    @Override
    protected HttpServerService serviceObject() {
        return (int port, int backlog) -> new TinyHttpServer(port, backlog);
    }
}
