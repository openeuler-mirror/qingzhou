package qingzhou.httpserver.impl.java;

import qingzhou.httpserver.HttpExchange;

import java.io.InputStream;
import java.io.OutputStream;

public class HttpExchangeImpl implements HttpExchange {
    private final com.sun.net.httpserver.HttpExchange httpExchange;

    public HttpExchangeImpl(com.sun.net.httpserver.HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
    }

    @Override
    public InputStream getRequestBody() {
        return httpExchange.getRequestBody();
    }

    @Override
    public OutputStream getResponseBody() {
        return httpExchange.getResponseBody();
    }
}
