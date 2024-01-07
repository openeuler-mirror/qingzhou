package qingzhou.httpserver.impl;

import qingzhou.framework.ServiceRegister;
import qingzhou.httpserver.HttpServerService;

public class Controller extends ServiceRegister<HttpServerService> {
    @Override
    protected Class<HttpServerService> serviceType() {
        return HttpServerService.class;
    }

    @Override
    protected HttpServerService serviceObject() {
        return new HttpServerServiceImpl();
    }
}
