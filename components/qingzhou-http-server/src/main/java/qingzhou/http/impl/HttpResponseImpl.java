package qingzhou.http.impl;

import java.nio.charset.StandardCharsets;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import qingzhou.http.server.HttpResponse;
import reactor.core.publisher.Sinks;
import reactor.netty.http.server.HttpServerResponse;

public class HttpResponseImpl implements HttpResponse {
    private final HttpServerResponse response;
    private final Sinks.Many<byte[]> streamResponse;

    public HttpResponseImpl(HttpServerResponse response, Sinks.Many<byte[]> streamResponse) {
        this.response = response;
        this.streamResponse = streamResponse;
    }

    private boolean used = false;

    private HttpResponse thisInstance() {
        used = true;
        return this;
    }

    public boolean isUsed() {
        return used;
    }

    @Override
    public void status500Finish(String msg) {
        response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        sendFinish(msg);
    }

    @Override
    public void status404Finish() {
        response.status(HttpResponseStatus.NOT_FOUND);
        finish();
    }

    @Override
    public void status400Finish() {
        response.status(HttpResponseStatus.BAD_REQUEST);
        finish();
    }

    @Override
    public HttpResponse status(int status) {
        response.status(status);
        return thisInstance();
    }

    @Override
    public HttpResponse header(String name, String value) {
        response.responseHeaders().set(name, value);
        return thisInstance();
    }

    @Override
    public HttpResponse contentType(String value) {
        response.responseHeaders().set(HttpHeaderNames.CONTENT_TYPE, value);
        return thisInstance();
    }

    @Override
    public HttpResponse send(String bodyAsUtf8) {
        if (bodyAsUtf8 != null) {
            send(bodyAsUtf8.getBytes(StandardCharsets.UTF_8));
        }
        return thisInstance();
    }

    @Override
    public HttpResponse send(byte[] body) {
        if (body != null) {
            Sinks.EmitResult result = streamResponse.tryEmitNext(body);
            if (result.isFailure()) {
                throw new RuntimeException("Client disconnected, cannot send data");
            }
        }
        return thisInstance();
    }

    @Override
    public void finish() {
        streamResponse.tryEmitComplete();
    }

    @Override
    public void sendFinish(String bodyAsUtf8) {
        send(bodyAsUtf8);
        finish();
    }

    @Override
    public void sendFinish(byte[] body) {
        send(body);
        finish();
    }
}
