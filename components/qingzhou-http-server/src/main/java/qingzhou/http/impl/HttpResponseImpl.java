package qingzhou.http.impl;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import qingzhou.http.server.HttpResponse;
import reactor.core.publisher.Sinks;
import reactor.netty.http.server.HttpServerResponse;

public class HttpResponseImpl implements HttpResponse {
    private final HttpServerResponse response;
    private final CompletableFuture<Void> headerSentFuture;
    private final Sinks.Many<byte[]> sendBodySink;

    public HttpResponseImpl(HttpServerResponse response, CompletableFuture<Void> headerSentFuture, Sinks.Many<byte[]> sendBodySink) {
        this.response = response;
        this.headerSentFuture = headerSentFuture;
        this.sendBodySink = sendBodySink;
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
    public HttpResponse contentTypeHtmlUtf8() {
        return contentType("text/html;charset=UTF-8");
    }

    @Override
    public HttpResponse contentTypeJsonUtf8() {
        return contentType("application/json;charset=UTF-8");
    }

    @Override
    public HttpResponse contentTypeStream() {
        return contentType("application/octet-stream");
    }

    @Override
    public HttpResponse sendResponse(String bodyAsUtf8) {
        if (bodyAsUtf8 == null) return this;
        return sendResponse(bodyAsUtf8.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public HttpResponse sendResponse(byte[] body) {
        if (body == null || body.length == 0) return this;
        headerSentFuture.complete(null);
        sendBodySink.tryEmitNext(body);
        return this;
    }
}
