package qingzhou.http.impl.sun;

import qingzhou.http.HttpContext;
import qingzhou.http.HttpHandler;

class HttpContextImpl implements HttpContext {
    private final com.sun.net.httpserver.HttpContext httpContext;

    HttpContextImpl(com.sun.net.httpserver.HttpContext httpContext) {
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
