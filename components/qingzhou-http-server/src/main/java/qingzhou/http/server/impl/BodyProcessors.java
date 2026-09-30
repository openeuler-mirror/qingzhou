package qingzhou.http.server.impl;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import qingzhou.http.server.HttpHandler;
import qingzhou.logger.Logger;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.ByteBufFlux;
import reactor.netty.Connection;
import reactor.util.concurrent.Queues;

import static qingzhou.http.server.util.Errors.getCause;
import static qingzhou.http.server.util.Errors.safeRun;

/**
 * 请求体处理：按闸门判定结果分流的聚合（进内存）与流式（handler 直收）两条链。
 * 两者互不调用，仅共享 RequestContext 与限额配置。
 */
final class BodyProcessors {
    private static final byte[] EMPTY_BODY = new byte[0];
    private static final int RESPONSE_QUEUE_SIZE = 1024; // 慢客户端时最多缓存的响应分片数

    private final long maxBodyBytes; // 单个请求体聚合进内存的上限
    private final long maxStreamBytes; // 流式上传总量上限
    private final Logger logger;

    BodyProcessors(long maxBodyBytes, long maxStreamBytes, Logger logger) {
        this.maxBodyBytes = maxBodyBytes;
        this.maxStreamBytes = maxStreamBytes;
        this.logger = logger;
    }

    Publisher<Void> handle(RequestContext ctx) {
        Sinks.Many<byte[]> streamResponse = Sinks.many().unicast()
                .onBackpressureBuffer(Queues.<byte[]>get(RESPONSE_QUEUE_SIZE).get()); // 有界缓冲：慢客户端不再导致内存无界堆积
        HttpResponseImpl httpResponse = new HttpResponseImpl(ctx.response, streamResponse);
        return ctx.streamRequired ? receiveStream(ctx, httpResponse, streamResponse)
                : aggregate(ctx, httpResponse, streamResponse);
    }

    private Publisher<Void> receiveStream(RequestContext ctx, HttpResponseImpl httpResponse, Sinks.Many<byte[]> streamResponse) {
        HttpHandler.StreamHandler streamHandler = ctx.streamHandler;
        Throwable beginError = safeRun(() -> streamHandler.onBegin(ctx.httpRequest, httpResponse));
        if (beginError != null) {
            logger.error("http handler error", beginError);
            safeRun(() -> streamHandler.onError(beginError)); // onBegin 可能已建临时文件，须给 handler 清理机会
            // handler 回调同步抛出时请求体还没读完，须先排空再回 500，否则残留字节会被当作下一个请求解析
            return ctx.respond(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        AtomicLong receivedBytes = new AtomicLong();
        AtomicBoolean bodyFullyRead = new AtomicBoolean();
        Disposable receiveDisposable = ctx.request.receive() // 下面开始 直接订阅原始数据流，不进行 聚合
                .doOnNext(byteBuf -> { // 限制上传总量，超限触发 onError，由 handler 清理临时文件并回错误
                    if (receivedBytes.addAndGet(byteBuf.readableBytes()) > maxStreamBytes) {
                        throw new BodyTooLargeException();
                    }
                })
                .subscribe(byteBuf -> {
                            byte[] bytes = new byte[byteBuf.readableBytes()];
                            byteBuf.readBytes(bytes);
                            try {
                                streamHandler.onNext(bytes); // 声明 throws Throwable，而 Consumer.accept 不得抛受检异常
                            } catch (Throwable e) {
                                throw Exceptions.propagate(e); // 致命 Error 原样抛出，其余包装后交给下方 err 分支收尾
                            }
                            // ByteBuf 由 Reactor Netty 框架负责释放，无需手动 release，否则引用计数提前耗尽
                        },
                        err -> {
                            Throwable cause = getCause(err);
                            safeRun(() -> streamHandler.onError(cause)); // 回调自身出错不得吞掉下方兜底
                            // 兜底：handler 若未发响应就返回，响应链永不结束，请求会一直挂到超时
                            if (!httpResponse.isCommitted()) {
                                logger.error("http stream handler error", cause); // 仅在真正需要兜底时记录，避免客户端中断也刷日志
                                HttpResponseStatus status = statusOf(cause);
                                httpResponse.status(status.code()).sendFinish(status.reasonPhrase());
                            }
                        },
                        () -> { // 完成信号：回调异常也必须收尾，否则 sink 不结束、许可与配额无法归还
                            bodyFullyRead.set(true);
                            Throwable cause = safeRun(streamHandler::onComplete);
                            if (cause != null) logger.error("http stream handler error", cause);
                            httpResponse.finish();
                        }
                );
        return ctx.response.sendByteArray(streamResponse.asFlux())
                .then()
                .doFinally(signal -> {
                    // 响应结束即停止读请求体：handler 在 onBegin 中就结束响应时，否则会继续读到 maxStreamBytes
                    receiveDisposable.dispose();
                    // 请求体未读完就结束响应：残留字节会在保活连接上被当作下一个请求解析（请求走私），必须断开
                    if (!bodyFullyRead.get()) ctx.response.withConnection(Connection::dispose);
                });
    }

    private Publisher<Void> aggregate(RequestContext ctx, HttpResponseImpl httpResponse, Sinks.Many<byte[]> streamResponse) {
        HttpHandler httpHandler = ctx.entry.handler;
        AtomicLong receivedBytes = new AtomicLong();
        return ByteBufFlux.fromInbound(ctx.request.receive()
                        .doOnNext(byteBuf -> { // chunked 请求无 Content-Length，聚合前按实际字节数二次限制
                            if (receivedBytes.addAndGet(byteBuf.readableBytes()) > maxBodyBytes) {
                                throw new BodyTooLargeException();
                            }
                        }))
                .aggregate().asByteArray() // 所有输入 聚合 到一起再发送给订阅者
                .defaultIfEmpty(EMPTY_BODY)
                .flatMap(bytes -> {
                    try {
                        ctx.httpRequest.setRequestBody(bytes);
                        httpHandler.handle(ctx.httpRequest, httpResponse);
                        // 幂等：handler 未收尾（含 send 后漏 finish）时补上终止信号，避免请求无限等
                        httpResponse.finish();
                    } catch (Throwable e) {
                        Throwable cause = getCause(e);
                        // 客户端中断会让 sink 发射失败，属正常终止：逐条记 ERROR 会被放大为日志洪水
                        if (!httpResponse.isCancelled()) logger.error("http handler error", cause);
                        if (httpResponse.isCommitted()) {
                            httpResponse.finish(); // 已提交，状态码无法回退，只能干净收尾
                        } else {
                            HttpResponseStatus status = statusOf(cause);
                            httpResponse.status(status.code()).sendFinish(status.reasonPhrase());
                        }
                    }
                    return ctx.response.sendByteArray(streamResponse.asFlux()).then();
                })
                // 连接中断等也应回响应，且不能一律报 413；已写出响应体时不得再发状态行，否则造成响应污染
                .onErrorResume(e -> {
                    if (httpResponse.isCommitted()) {
                        httpResponse.finish(); // 已提交只能补终止信号，否则 sink 不结束、许可无法归还
                        return Mono.empty();
                    }
                    return ctx.respond(statusOf(getCause(e)));
                });
    }

    // 入参须为最内层 cause：Reactor 会包装异常，调用方先用 getCause 展开再传入，避免重复遍历
    private HttpResponseStatus statusOf(Throwable cause) {
        return cause instanceof BodyTooLargeException
                ? HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE
                : HttpResponseStatus.INTERNAL_SERVER_ERROR;
    }

    // 请求体超限的内部信号：仅用于在 Reactor 链上把 413 与 500 区分开，不对外暴露
    private static final class BodyTooLargeException extends RuntimeException {
        BodyTooLargeException() {
            super("request body too large");
        }
    }
}
