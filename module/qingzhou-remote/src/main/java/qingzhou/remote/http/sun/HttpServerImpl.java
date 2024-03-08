package qingzhou.remote.http.sun;

import qingzhou.remote.http.HttpContext;
import qingzhou.remote.http.HttpServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class HttpServerImpl implements HttpServer {
    private com.sun.net.httpserver.HttpServer httpServer;

    @Override
    public void start(String host, int port, int backlog) throws Exception {
        httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(InetAddress.getByName(host), port), backlog);
        httpServer.start();
    }

    @Override
    public void stop(int delaySeconds) {
        httpServer.stop(delaySeconds);
    }

    @Override
    public HttpContext createContext(String contextPath) {
        return new HttpContextImpl(httpServer.createContext(contextPath));
    }

    @Override
    public void removeContext(String contextPath) {
        httpServer.removeContext(contextPath);
    }
}
