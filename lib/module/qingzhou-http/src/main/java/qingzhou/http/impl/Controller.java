package qingzhou.http.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.http.Http;
import qingzhou.http.HttpClient;
import qingzhou.http.HttpServer;
import qingzhou.http.impl.sun.HttpServerImpl;

@Module
public class Controller implements ModuleActivator {
    @Override
    public void start(ModuleContext context) {
        context.registerService(Http.class, new Http() {
            @Override
            public HttpServer buildHttpServer() {
                return new HttpServerImpl();
            }

            @Override
            public HttpClient buildHttpClient() {
                return new HttpClientImpl();
            }
        });
    }
}
