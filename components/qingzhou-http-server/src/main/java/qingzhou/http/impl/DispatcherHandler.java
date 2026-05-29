package qingzhou.http.impl;

import java.util.function.BiFunction;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import qingzhou.http.server.HttpHandler;
import qingzhou.logger.Logger;
import reactor.core.publisher.Sinks;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

class DispatcherHandler implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {
    private static final byte[] NULL_BYTES = new byte[0];
    private final HttpServerImpl httpServer;
    private final long maxBodySize;
    private final Logger logger;

    DispatcherHandler(HttpServerImpl httpServer, long maxBodySize, Logger logger) {
        this.httpServer = httpServer;
        this.maxBodySize = maxBodySize;
        this.logger = logger;
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        String requestPath = request.uri().split("\\?")[0];
        String normalizedPath = requestPath.endsWith("/") ? requestPath : requestPath + "/";
        String matches = httpServer.matches(normalizedPath);
        if (matches == null) {
            return response
                    .status(HttpResponseStatus.NOT_FOUND)
                    .send();
        }
        HttpHandler httpHandler = this.httpServer.handlerMap.get(matches);
        HttpHandler.StreamHandler streamHandler = httpHandler.getStreamHandler();

        boolean streamRequired = false;
        if (request.method() == HttpMethod.POST) {
            streamRequired = true;
            // 检查 Content-Length
            String contentLength = request.requestHeaders().get(HttpHeaderNames.CONTENT_LENGTH);
            if (contentLength != null) {
                try {
                    long len = Long.parseLong(contentLength);
                    if (len < maxBodySize) {
                        streamRequired = false;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (streamRequired && streamHandler == null) {
            return response
                    .status(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE)
                    .send();
        }

        Sinks.Many<byte[]> streamResponse = Sinks.many().unicast().onBackpressureBuffer();
        HttpRequestImpl httpRequest = new HttpRequestImpl(request, requestPath);
        HttpResponseImpl httpResponse = new HttpResponseImpl(response, streamResponse);

        if (streamRequired) {
            request.receive() // 下面开始 直接订阅原始数据流，不进行 聚合
                    .subscribe(byteBuf -> {
                                byte[] bytes = new byte[byteBuf.readableBytes()];
                                byteBuf.readBytes(bytes);
                                byteBuf.release(); // 重要：释放资源，防止内存泄漏
                                streamHandler.onNext(bytes);
                            },
                            err -> streamHandler.onError(err),
                            () -> streamHandler.onComplete() // 完成信号
                    );
            return response.sendByteArray(streamResponse.asFlux()).then();
        } else {
            return request.receive()
                    .aggregate().asByteArray() // 所有输入 聚合 到一起再发送给订阅者
                    .defaultIfEmpty(NULL_BYTES)
                    .flatMap(bytes -> {
                        try {
                            httpRequest.setRequestBody(bytes);
                            httpHandler.handle(httpRequest, httpResponse);
                            if (!httpResponse.isUsed()) {
                                streamResponse.tryEmitComplete(); // 避免请求无限等
                            }
                        } catch (Throwable e) {
                            response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                            Throwable cause = getCause(e);
                            logger.error("http handler error", cause);
                            streamResponse.tryEmitError(cause);
                        }
                        return response.sendByteArray(streamResponse.asFlux()).then();
                    });
        }
    }

    private Throwable getCause(Throwable e) {
        Throwable cause = e;

        while (cause != null
                && cause.getCause() != null
                && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        return cause;
    }
}
