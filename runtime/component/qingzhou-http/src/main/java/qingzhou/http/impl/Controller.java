package qingzhou.http.impl;

import qingzhou.engine.ServiceRegister;
import qingzhou.http.HttpServer;
import qingzhou.http.impl.sun.HttpServerImpl;

public class Controller extends ServiceRegister<HttpServer> {
    @Override
    public Class<HttpServer> serviceType() {
        return HttpServer.class;
    }

    @Override
    protected HttpServer serviceObject() {
        return new HttpServerImpl();
    }
}
