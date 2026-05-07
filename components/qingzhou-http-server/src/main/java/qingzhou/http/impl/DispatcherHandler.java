package qingzhou.http.impl;

import java.util.function.BiFunction;

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
    private final Logger logger;

    DispatcherHandler(HttpServerImpl httpServer, Logger logger) {
        this.httpServer = httpServer;
        this.logger = logger;
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        String requestPath = request.uri().split("\\?")[0];
        String normalizedPath = requestPath.endsWith("/") ? requestPath : requestPath + "/";
        String matches = httpServer.matches(normalizedPath);
        if (matches == null) return response.status(HttpResponseStatus.NOT_FOUND);

        HttpHandler httpHandler = this.httpServer.handlerMap.get(matches);
        return request.receive()
                .aggregate().asByteArray()
                .defaultIfEmpty(NULL_BYTES)
                .flatMap(bytes -> {
                    Sinks.Many<byte[]> streamResponse = Sinks.many().unicast().onBackpressureBuffer();
                    try {
                        HttpRequestImpl httpRequest = new HttpRequestImpl(request,
                                requestPath, bytes);
                        HttpResponseImpl httpResponse = new HttpResponseImpl(response, streamResponse);
                        httpHandler.handle(httpRequest, httpResponse);
                        if (!httpResponse.isUsed()) {
                            streamResponse.tryEmitComplete(); // 避免请求无限等
                        }
                    } catch (Throwable e) {
                        response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        Throwable cause = getCause(e);
                        logger.error("http processing error", cause);
                        streamResponse.tryEmitError(cause);
                    }
                    return response.sendByteArray(streamResponse.asFlux()).then();
                });
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
