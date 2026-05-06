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

    @Override
    public HttpResponse statusError() {
        response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        return this;
    }

    @Override
    public HttpResponse statusNotFound() {
        response.status(HttpResponseStatus.NOT_FOUND);
        return this;
    }

    @Override
    public HttpResponse statusBad() {
        response.status(HttpResponseStatus.BAD_REQUEST);
        return this;
    }

    @Override
    public HttpResponse status(int status) {
        response.status(status);
        return this;
    }

    @Override
    public HttpResponse header(String name, String value) {
        response.responseHeaders().set(name, value);
        return this;
    }

    @Override
    public HttpResponse contentType(String value) {
        response.responseHeaders().set(HttpHeaderNames.CONTENT_TYPE, value);
        return this;
    }

    @Override
    public HttpResponse contentTypeJsonUtf8() {
        return contentType("application/json;charset=UTF-8");
    }

    @Override
    public HttpResponse send(String bodyAsUtf8) {
        if (bodyAsUtf8 != null) {
            send(bodyAsUtf8.getBytes(StandardCharsets.UTF_8));
        }
        return this;
    }

    @Override
    public HttpResponse send(byte[] body) {
        if (body != null) {
            streamResponse.tryEmitNext(body);
        }
        return this;
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
