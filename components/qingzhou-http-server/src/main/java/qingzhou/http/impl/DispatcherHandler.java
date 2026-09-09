package qingzhou.http.impl;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpHandler;
import qingzhou.logger.Logger;
import reactor.core.publisher.Mono;
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
        try {
            requestPath = URLDecoder.decode(requestPath, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return response.status(HttpResponseStatus.BAD_REQUEST).send();
        }
        requestPath = normalize(requestPath);
        if (requestPath == null) { // 含 ".." 段一律拒绝：handler 各自解析路径，无法保证都能防住穿越
            return response.status(HttpResponseStatus.BAD_REQUEST).send();
        }
        HttpRequestImpl httpRequest = new HttpRequestImpl(request, requestPath);

        HttpHandler httpHandler = httpServer.findHandler(requestPath);
        if (httpHandler == null) {
            return response.status(HttpResponseStatus.NOT_FOUND).send();
        }

        // 安全认证
        boolean needAuth = !httpServer.isAuthDisabled && !httpServer.noAuthHandlerSet.contains(httpHandler);
        if (needAuth) {
            AuthResult authResult = httpServer.authenticate(httpRequest);
            if (authResult.status() != AuthResult.Status.PASS) {
                return response.status(HttpResponseStatus.UNAUTHORIZED)
                        .header("WWW-Authenticate", "Bearer")
                        .header("Cache-Control", "no-store")
                        .sendString(Mono.just("Unauthorized"));
            }
            if (authResult.getPrincipal() != null) {
                httpRequest.setAttribute(AuthResult.AUTH_PRINCIPAL_USERNAME_ATTRIBUTE, authResult.getPrincipal());
            }
        }

        // 开始处理业务...
        HttpHandler.StreamHandler streamHandler = httpHandler.buildStreamHandler();
        boolean streamRequired = request.method() == HttpMethod.POST && request.isMultipart();
        if (streamRequired && streamHandler == null) {
            return response.status(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE).send();
        }

        addSecurityHeaders(response);
        Sinks.Many<byte[]> streamResponse = Sinks.many().unicast().onBackpressureBuffer();
        HttpResponseImpl httpResponse = new HttpResponseImpl(response, streamResponse);

        if (streamRequired) {
            streamHandler.onBegin(httpRequest, httpResponse);
            request.receive() // 下面开始 直接订阅原始数据流，不进行 聚合
                    .subscribe(byteBuf -> {
                                byte[] bytes = new byte[byteBuf.readableBytes()];
                                byteBuf.readBytes(bytes);
                                streamHandler.onNext(bytes);

                                // Reactor Netty 对 ByteBuf 的生命周期管理遵循「发布者负责释放，订阅者负责引用计数」的原则：
                                //request.receive() 产生的 ByteBuf 由 Reactor Netty 框架管理，框架会在数据处理完成后自动释放；
                                //手动调用 byteBuf.release() 会导致 ByteBuf 的引用计数被提前耗尽，可能引发两种严重问题：
                                //重复释放（Double Release）：框架后续尝试释放已被手动释放的 ByteBuf，触发 IllegalReferenceCountException；
                                // byteBuf.release();
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

    /**
     * 路径规范化：折叠重复斜杠与 "." 段；发现 ".." 段返回 null（拒绝请求）。
     * 各 handler 自行解析路径，防护难以统一，故在分发入口一次性拦截穿越。
     */
    private static String normalize(String path) {
        StringBuilder normalized = new StringBuilder(path.length());
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || segment.equals(".")) continue;
            if (segment.equals("..")) return null;
            normalized.append('/').append(segment);
        }
        if (path.endsWith("/") && normalized.length() > 0) normalized.append('/');
        return normalized.length() == 0 ? "/" : normalized.toString();
    }

    private void addSecurityHeaders(HttpServerResponse response) {
        response.responseHeaders()
                .set("X-Content-Type-Options", "nosniff")
                .set("X-Frame-Options", "SAMEORIGIN")
                .set("Referrer-Policy", "no-referrer");
        if (httpServer.isSslEnabled) {
            response.responseHeaders().set("Strict-Transport-Security", "max-age=31536000");
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
