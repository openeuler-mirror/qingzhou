package qingzhou.remote.http.sun;

import qingzhou.remote.http.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HttpExchangeImpl implements HttpExchange {
    private final com.sun.net.httpserver.HttpExchange httpExchange;

    public HttpExchangeImpl(com.sun.net.httpserver.HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
    }

    @Override
    public String getRequestURI() {
        return String.valueOf(httpExchange.getRequestURI());
    }

    @Override
    public InputStream getRequestBody() {
        return httpExchange.getRequestBody();
    }

    @Override
    public void setStatus(int status) throws IOException {
        httpExchange.sendResponseHeaders(status, 0);
    }

    @Override
    public void addResponseHeader(String name, String value) {
        httpExchange.getResponseHeaders().add(name, value);
    }

    @Override
    public OutputStream getResponseBody() {
        return httpExchange.getResponseBody();
    }

    @Override
    public void close() {
        httpExchange.close();
    }
}
