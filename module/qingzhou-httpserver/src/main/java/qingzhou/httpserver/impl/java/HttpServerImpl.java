package qingzhou.httpserver.impl.java;

import qingzhou.httpserver.HttpHandler;
import qingzhou.httpserver.HttpServer;

public class HttpServerImpl implements HttpServer {
    private final com.sun.net.httpserver.HttpServer httpServer;

    public HttpServerImpl(com.sun.net.httpserver.HttpServer httpServer) {
        this.httpServer = httpServer;
    }

    @Override
    public void addContext(String path, HttpHandler httpHandler) {
        httpServer.createContext(path, httpExchange -> httpHandler.handle(new HttpExchangeImpl(httpExchange)));
    }

    @Override
    public void removeContext(String path) {
        httpServer.removeContext(path);
    }

    @Override
    public void start() {
        httpServer.start();
    }

    @Override
    public void stop(int delaySeconds) {
        httpServer.stop(delaySeconds);
    }
}
