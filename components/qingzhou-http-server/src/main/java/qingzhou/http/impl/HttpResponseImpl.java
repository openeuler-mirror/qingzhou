package qingzhou.http.impl;

import java.nio.charset.StandardCharsets;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import qingzhou.http.server.HttpResponse;
import reactor.core.publisher.Sinks;
import reactor.netty.http.server.HttpServerResponse;

class HttpResponseImpl implements HttpResponse {
    private final HttpServerResponse response;
    private final Sinks.Many<byte[]> streamResponse;

    HttpResponseImpl(HttpServerResponse response, Sinks.Many<byte[]> streamResponse) {
        this.response = response;
        this.streamResponse = streamResponse;
    }

    // handler 可能在业务线程写、框架在 EventLoop 读，须保证可见性
    private volatile boolean committed = false;
    private volatile boolean cancelled = false;

    // 订阅方已取消（客户端断开）：属正常终止，不得按错误处理
    boolean isCancelled() {
        return cancelled;
    }

    /**
     * 响应是否已提交（写入过响应体，或已发出终止信号）。
     * 提交后状态码与响应头不可再更改，框架只能做干净收尾。
     * 仅 send/finish 置位：status/header/contentType 之后仍允许错误分支改写状态码。
     */
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void status500Finish(String msg) {
        response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        sendFinish(msg);
    }

    @Override
    public void status400Finish() {
        response.status(HttpResponseStatus.BAD_REQUEST);
        finish();
    }

    @Override
    public HttpResponse status(int status) {
        if (status < 100 || status > 599) { // 非法状态码会让 Netty 输出无意义的状态行
            throw new IllegalArgumentException("invalid http status: " + status);
        }
        response.status(status);
        return this;
    }

    @Override
    public HttpResponse header(String name, String value) {
        response.header(name, value);
        return this;
    }

    @Override
    public void redirect(String url) {
        response.status(HttpResponseStatus.FOUND)
                .header(HttpHeaderNames.LOCATION, url)
                .header(HttpHeaderNames.CONTENT_LENGTH, HttpHeaderValues.ZERO);
        finish();
    }

    @Override
    public HttpResponse contentType(String value) {
        response.header(HttpHeaderNames.CONTENT_TYPE, value);
        return this;
    }

    @Override
    public HttpResponse contentTypeJsonUtf8() {
        return contentType("application/json; charset=utf-8");
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
            Sinks.EmitResult result = streamResponse.tryEmitNext(body);
            // 失败原因可能是客户端断开、背压缓冲满、已结束或并发发射，须如实暴露而非一律归为断连
            if (result.isFailure()) {
                cancelled = result == Sinks.EmitResult.FAIL_CANCELLED;
                throw new IllegalStateException("cannot emit response body: " + result);
            }
            committed = true;
        }
        return this;
    }

    @Override
    public void finish() {
        committed = true;
        streamResponse.tryEmitComplete(); // 已取消或已结束时忽略，重复调用无副作用
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
