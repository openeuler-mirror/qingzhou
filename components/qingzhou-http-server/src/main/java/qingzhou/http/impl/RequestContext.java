package qingzhou.http.impl;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import qingzhou.http.server.HttpHandler;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

/**
 * 一次请求在准入闸门与 body 处理之间传递的状态：每个 gate 只写自己负责的字段。
 */
final class RequestContext {
    final HttpServerRequest request;
    final HttpServerResponse response;
    final AtomicBoolean released = new AtomicBoolean();
    // 取得许可时的信号量快照：OSGi 热更新会重建信号量，归还必须回到取得许可的那一个，
    // 否则旧请求的归还会让新信号量超发、新配置的限流暂时失真
    final Semaphore concurrentSemaphore;
    final Semaphore streamSemaphore;

    String path; // 规范化后的路径
    HandlerManager.HandlerEntry entry;
    HttpRequestImpl httpRequest;
    HttpHandler.StreamHandler streamHandler;
    boolean streamRequired;
    boolean streamPermitHeld; // 流式分支已取得的并发许可
    long reservedBytes; // 聚合分支已预留的内存配额

    RequestContext(HttpServerRequest request, HttpServerResponse response,
                   Semaphore concurrentSemaphore, Semaphore streamSemaphore) {
        this.request = request;
        this.response = response;
        this.concurrentSemaphore = concurrentSemaphore;
        this.streamSemaphore = streamSemaphore;
    }

    /**
     * 终止响应一律不读请求体并关闭连接：
     * 既避免错误分支被当成免费上传通道，也避免残留请求体在保活连接上被当作下一个请求解析（请求走私）。
     */
    Mono<Void> respond(HttpResponseStatus status) {
        return response.status(status)
                .header(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                .send();
    }
}
