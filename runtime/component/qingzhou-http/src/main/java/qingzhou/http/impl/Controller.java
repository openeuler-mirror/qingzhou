package qingzhou.http.impl;

import qingzhou.engine.ServiceRegister;
import qingzhou.http.Http;
import qingzhou.http.HttpClient;
import qingzhou.http.HttpServer;
import qingzhou.http.impl.sun.HttpServerImpl;

public class Controller extends ServiceRegister<Http> {
    @Override
    public Class<Http> serviceType() {
        return Http.class;
    }

    @Override
    protected Http serviceObject() {
        return new Http() {
            @Override
            public HttpServer buildHttpServer() {
                return new HttpServerImpl();
            }

            @Override
            public HttpClient buildHttpClient() {
                return new HttpClientImpl();
            }
        };
    }
}
