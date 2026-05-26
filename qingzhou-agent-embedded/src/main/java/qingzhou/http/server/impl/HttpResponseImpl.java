package qingzhou.http.server.impl;

import com.sun.net.httpserver.HttpExchange;
import qingzhou.http.server.HttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class HttpResponseImpl implements HttpResponse {
    private final HttpExchange exchange;
    private int status = 200;
    private boolean finished;

    HttpResponseImpl(HttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void status500Finish(String msg) {
        try {
            status(500);
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, data.length);
            OutputStream os = exchange.getResponseBody();
            os.write(data);
            os.close();
            finished = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void status404Finish() {
        try {
            exchange.sendResponseHeaders(404, -1);
            finished = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void status400Finish() {
        try {
            exchange.sendResponseHeaders(400, -1);
            finished = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public HttpResponse status(int status) {
        this.status = status;
        return this;
    }

    @Override
    public HttpResponse header(String name, String value) {
        exchange.getResponseHeaders().add(name, value);
        return this;
    }

    @Override
    public HttpResponse contentType(String value) {
        exchange.getResponseHeaders().set("Content-Type", value);
        return this;
    }

    @Override
    public HttpResponse send(String bodyAsUtf8) {
        return send(bodyAsUtf8.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public HttpResponse send(byte[] body) {
        try {
            exchange.sendResponseHeaders(status, body.length);
            OutputStream os = exchange.getResponseBody();
            os.write(body);
            os.close();
            finished = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public void finish() {
        if (!finished) {
            try {
                exchange.sendResponseHeaders(status, -1);
                finished = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendFinish(String bodyAsUtf8) {
        send(bodyAsUtf8);
    }

    @Override
    public void sendFinish(byte[] body) {
        send(body);
    }

    public boolean isFinished() {
        return finished;
    }
}