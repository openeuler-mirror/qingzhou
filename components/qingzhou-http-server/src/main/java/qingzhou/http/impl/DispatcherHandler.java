package qingzhou.http.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiFunction;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import qingzhou.http.server.HttpHandler;
import qingzhou.logger.Logger;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

class DispatcherHandler implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {
    private static final byte[] NULL_BYTES = new byte[0];
    private final HttpServerImpl httpServerImpl;
    private final ThreadPoolExecutor taskThreadPool;
    private final Logger logger;

    DispatcherHandler(HttpServerImpl httpServerImpl, ThreadPoolExecutor taskThreadPool, Logger logger) {
        this.httpServerImpl = httpServerImpl;
        this.taskThreadPool = taskThreadPool;
        this.logger = logger;
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        String requestPath = request.uri().split("\\?")[0];
        String normalizedPath = requestPath.endsWith("/") ? requestPath : requestPath + "/";
        String matches = httpServerImpl.matches(normalizedPath);
        if (matches == null) return response.status(HttpResponseStatus.NOT_FOUND);

        HttpHandler httpHandler = this.httpServerImpl.handlerMap.get(matches);
        return request.receive()
                .aggregate().asByteArray()
                .defaultIfEmpty(NULL_BYTES)
                .flatMap(bytes -> {
                    CompletableFuture<Void> headerSentFuture = new CompletableFuture<>();
                    Sinks.Many<byte[]> sendBodySink = Sinks.many().unicast().onBackpressureBuffer();

                    taskThreadPool.execute(() -> {
                        logger.info("Incoming request: " + request.uri());
                        try {
                            HttpRequestImpl httpRequest = new HttpRequestImpl(request,
                                    requestPath, bytes);
                            HttpResponseImpl httpResponse = new HttpResponseImpl(response,
                                    headerSentFuture, sendBodySink);
                            httpHandler.handle(httpRequest, httpResponse);
                        } catch (Throwable e) {
                            response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                            Throwable cause = getCause(e);
                            logger.error("An exception occurred during HTTP processing.", cause);
                        } finally {
                            headerSentFuture.complete(null);
                            sendBodySink.tryEmitComplete();
                        }
                    });

                    return Mono.fromFuture(headerSentFuture)
                            .then(Mono.defer(() -> response.sendByteArray(sendBodySink.asFlux()).then()));
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
