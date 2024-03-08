package qingzhou.remote.http.sun;

import qingzhou.remote.http.HttpContext;
import qingzhou.remote.http.HttpHandler;

public class HttpContextImpl implements HttpContext {
    private final com.sun.net.httpserver.HttpContext httpContext;

    public HttpContextImpl(com.sun.net.httpserver.HttpContext httpContext) {
        this.httpContext = httpContext;
    }

    @Override
    public void setHandler(HttpHandler handler) {
        httpContext.setHandler(httpExchange -> handler.handle(new HttpExchangeImpl(httpExchange)));
    }

    @Override
    public String getPath() {
        return httpContext.getPath();
    }
}
