package qingzhou.http.server.impl;

import java.nio.charset.StandardCharsets;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import qingzhou.http.server.HttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.concurrent.Queues;

class HttpResponseImpl implements HttpResponse {
    private static final int RESPONSE_QUEUE_SIZE = 1024; // 慢客户端时最多缓存的响应分片数

    private final HttpServerResponse response;
    private final Sinks.Many<byte[]> streamResponse;
    private final Sinks.Empty<Void> firstOutputSignal = Sinks.empty();

    HttpResponseImpl(HttpServerResponse response) {
        this.response = response;
        // 有界缓冲：慢客户端不再导致内存无界堆积
        this.streamResponse = Sinks.many().unicast().onBackpressureBuffer(Queues.<byte[]>get(RESPONSE_QUEUE_SIZE).get());
    }

    // 流式链路须等首个响应内容产出后才订阅写链：订阅即发送状态行与响应头，
    // 提前订阅会让 handler 在 onComplete 里设置的 status/header/contentType 全部失效
    Mono<Void> firstOutput() {
        return firstOutputSignal.asMono();
    }

    Flux<byte[]> body() {
        return streamResponse.asFlux();
    }

    // handler 可能在业务线程写、框架在 EventLoop 读，须保证可见性
    private volatile boolean committed;
    private volatile boolean cancelled;
    private volatile boolean async;
    // handler 显式设置过 Content-Length：框架自动补不得覆盖业务意图
    private volatile boolean explicitContentLength;
    // 框架已补过 Content-Length：多次 send 只认首次，不重复补
    private volatile boolean contentLengthAutoApplied;
    private volatile int statusCode = 200;

    // 订阅方已取消（客户端断开）：属正常终止，不得按错误处理
    boolean isCancelled() {
        return cancelled;
    }

    // 调用方声明由自己异步收尾：框架返回后不得自动 finish
    boolean isAsync() {
        return async;
    }

    /**
     * 响应是否已提交（写入过响应体，或已发出终止信号）。
     * 提交后状态码与响应头不可再更改，框架只能做干净收尾。
     * 仅 send/finish 置位：status/header/contentType 之后仍允许错误分支改写状态码。
     */
    boolean isCommitted() {
        return committed;
    }

    @Override
    public HttpResponse status(int status) {
        if (status < 100 || status > 599) { // 非法状态码会让 Netty 输出无意义的状态行
            throw new IllegalArgumentException("invalid http status: " + status);
        }
        response.status(status);
        statusCode = status;
        return this;
    }

    @Override
    public HttpResponse header(String name, String value) {
        // 头名或值含 CR/LF/NUL 会构造出额外的响应头或响应体（响应头注入）
        if (name == null || value == null || hasBreak(name) || hasBreak(value)) {
            throw new IllegalArgumentException("invalid http header");
        }
        if ("Content-Length".equalsIgnoreCase(name)) explicitContentLength = true;
        response.header(name, value);
        return this;
    }

    private static boolean hasBreak(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r' || c == '\n' || c == '\0') return true;
        }
        return false;
    }

    @Override
    public HttpResponse contentType(String value) {
        if (value == null || hasBreak(value)) throw new IllegalArgumentException("invalid content type");
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
            // 单次 send 已是完整实体：补 Content-Length 走定长，对齐 Servlet 容器行为并消除 chunked 开销。
            // 声明 async 或多次 send 时总量不可知，交给 Netty 按 chunked 处理
            if (!committed && !async && !explicitContentLength && !contentLengthAutoApplied) {
                applyContentLength(body.length);
            }
            Sinks.EmitResult result = streamResponse.tryEmitNext(body);
            // 失败原因可能是客户端断开、背压缓冲满、已结束或并发发射，须如实暴露而非一律归为断连
            if (result.isFailure()) {
                cancelled = result == Sinks.EmitResult.FAIL_CANCELLED;
                throw new IllegalStateException("cannot emit response body: " + result);
            }
            committed = true;
            firstOutputSignal.tryEmitEmpty();
        }
        return this;
    }

    @Override
    public HttpResponse async() {
        async = true;
        return this;
    }

    @Override
    public void finish() {
        // 未写实体且无显式长度：明确宣告空响应，避免 Netty 对空 Flux 仍走 chunked
        if (!committed && !async && !explicitContentLength && !contentLengthAutoApplied) {
            applyContentLength(0);
        }
        committed = true;
        streamResponse.tryEmitComplete(); // 已取消或已结束时忽略，重复调用无副作用
        firstOutputSignal.tryEmitEmpty();
    }

    // RFC 7230 禁止 1xx/204/304 携带 Content-Length；无论是否写头都标记已处理，避免重复尝试
    private void applyContentLength(long length) {
        if (statusCode >= 200 && statusCode != 204 && statusCode != 304) {
            response.header(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(length));
        }
        contentLengthAutoApplied = true;
    }

    @Override
    public void redirect(String url) {
        // 双防线：CRLF 防响应头注入；仅允许站内相对地址防开放重定向（放行进站外绝对地址即构成开放重定向）。
        // 相对地址须排除 "//host"（协议相对）与任何反斜杠：WHATWG 解析把 \ 规范化为 /，
        // "/\evil.com" 会被浏览器当作协议相对地址跳到外站。
        // TAB 同样必须拦截：WHATWG 解析会先移除 URL 中的制表符，"/\t/evil.com" 会被还原成 "//evil.com"
        boolean validTarget = url != null && !url.isEmpty() && url.indexOf('\\') < 0 && url.indexOf('\t') < 0
                // && url.startsWith("/") // oauth2 重定向是 http 开头的外部地址
                && !url.startsWith("//");
        if (!validTarget || hasBreak(url)) throw new IllegalArgumentException("invalid redirect url");
        response.status(HttpResponseStatus.FOUND)
                .header(HttpHeaderNames.LOCATION, url);
        header("Content-Length", "0"); // 走统一入口：标记显式长度，finish 不再重复补
        finish();
    }
}
