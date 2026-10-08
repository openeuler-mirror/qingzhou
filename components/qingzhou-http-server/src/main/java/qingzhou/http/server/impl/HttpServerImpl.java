package qingzhou.http.server.impl;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import org.osgi.service.component.annotations.*;
import qingzhou.crypto.Crypto;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpServer;
import qingzhou.logger.Logger;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.resources.LoopResources;

import static qingzhou.http.server.util.Configs.*;

@Component(immediate = true, configurationPid = "qingzhou-http-server", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class HttpServerImpl implements HttpServer {
    @Reference
    private Crypto crypto;
    @Reference
    private Logger logger;
    @Reference
    private HandlerManager handlerManager;
    @Reference
    private DispatcherHandler dispatcherHandler;

    private LoopResources loopResources;
    private DisposableServer disposableServer;

    @Activate
    public void start(Map<String, String> config) throws Exception {
        int selectorThreads = atLeast(config, "selector", 1, 1);
        int workerThreads = atLeast(config, "worker", 1, Runtime.getRuntime().availableProcessors() * 2);
        // 默认 60 秒：SSE 等长连接在两个数据包之间可能长时间静默，过低会切断正常业务；0 表示不启用空闲超时
        int idleTimeout = atLeast(config, "idle_timeout", 0, 60);
        String host = getConfig(config, "host", "127.0.0.1");
        int port = port(config, "port");

        // 创建可复用的 EventLoop 资源（生产必备：避免线程池重复创建，支持优雅关闭）
        loopResources = LoopResources.create(
                "http-server",  // 线程名称前缀（方便排查）
                selectorThreads,      // Boss线程数
                workerThreads,        // Worker线程数
                true                  // 是否为守护线程（生产建议true，不阻塞应用退出）
        );
        // 构建生产级 HTTP 服务（配置超时、线程池、TCP 选项）
        reactor.netty.http.server.HttpServer httpServer = reactor.netty.http.server.HttpServer.create()
                .host(host)
                .port(port)
                .runOn(loopResources)
                // TCP 底层配置：限制连接队列并启用存活探测
                .option(ChannelOption.SO_REUSEADDR, true) // tcp 层端口复用（高效，但有数据混乱低风险，因netty等框架有容错检测故可打开）
                .option(ChannelOption.SO_BACKLOG, 1024) // tcp 层连接队列，应对突发流量避免客户端被拒绝，过大会消耗系统资源
                .childOption(ChannelOption.SO_KEEPALIVE, true) // tcp 层保活探测，避免对方意外断电等资源无效占用
                .childOption(ChannelOption.TCP_NODELAY, true) // 现代带宽充足，路由器处理能力强，「小包风暴」的影响远小于实时性不足带来的业务问题
                .httpRequestDecoder(spec -> spec.maxInitialLineLength(4096).maxHeaderSize(8192)) // 显式收敛请求行/头，不依赖框架默认值（升级即行为漂移）
                .handle(dispatcherHandler); // 业务路由

        // 一条连接无任何读写活动则关闭释放资源；0 表示不启用，留给 SSE 等长连接
        if (idleTimeout > 0) {
            httpServer = httpServer.idleTimeout(Duration.ofSeconds(idleTimeout));
        }

        // 单连接保活请求数上限：防止慢连接长期占用同一连接的请求配额
        int maxKeepAliveRequests = atLeast(config, "max_keep_alive_requests", 1, 100);
        httpServer = httpServer.maxKeepAliveRequests(maxKeepAliveRequests);

        // 连接总数上限（0 表示不限）：限制实际 TCP 连接数，防慢连接洪水占满连接槽。
        // 业内常见做法：在连接建立处计数，超限即关闭新连接；计数以连接关闭为唯一归还点，避免计数漂移。
        int maxConnections = atLeast(config, "max_connections", 0, 10_000);
        if (maxConnections > 0) {
            AtomicInteger connections = new AtomicInteger();
            httpServer = httpServer.doOnConnection(connection -> {
                // 先注册归还回调：被拒连接同样要走这条路，否则计数只增不减，最终永久拒绝所有连接
                connection.channel().closeFuture().addListener(future -> connections.decrementAndGet());
                if (connections.incrementAndGet() > maxConnections) {
                    connection.channel().close(); // 超限：立即关闭，不占用 EventLoop 与请求配额
                }
            });
        }

        // 密钥库校验必须在绑定端口前完成，任一配置错误都应直接启动失败且不监听端口
        boolean sslEnabled = getConfig(config, "ssl_enabled", true);
        boolean allowPlaintext = getConfig(config, "allow_plaintext", false);
        boolean loopbackOnly = "127.0.0.1".equals(host) || "localhost".equals(host) || "::1".equals(host);
        if (!sslEnabled && !loopbackOnly && !allowPlaintext) {
            // 非环回地址上的明文会让口令与令牌暴露在链路上；经 TLS 反代暴露时用 allow_plaintext 显式确认
            throw new IllegalStateException("ssl_enabled must be true when host is not loopback: " + host
                    + " (or set qingzhou-http-server.allow_plaintext=true when served only via a trusted TLS proxy)");
        }
        try {
            if (sslEnabled) {
                SslContext sslContext = SslContextBuilder.buildSslContext(config, crypto.getGlobalCipher(), logger);
                httpServer = httpServer.secure(spec -> spec.sslContext(sslContext));
            }
            disposableServer = httpServer.bindNow();
        } catch (Exception e) {
            disposeOnStartFailure(e); // 激活失败时 @Deactivate 不会被调用，已创建的 EventLoop 须在此回收
            throw e;
        }

        // 用实际端口：port=0 时由操作系统分配，配置里的 0 无法用于排障
        if (!sslEnabled) logger.warn("http server is using plaintext HTTP; enable TLS before remote exposure");
        logger.info("http server started: " + (sslEnabled ? "https" : "http") + "://" + host + ":" + disposableServer.port() + "/web");
    }

    @Deactivate
    public void stop() {
        disposeResources();
        logger.info("http server stopped");
    }

    // 释放失败不得掩盖真正的启动失败原因，否则排障看到的是释放异常而非端口/密钥库问题
    private void disposeOnStartFailure(Exception startError) {
        try {
            disposeResources();
        } catch (Exception releaseError) {
            startError.addSuppressed(releaseError);
        }
    }

    // 两类资源独立收尾：服务端关闭失败也必须继续回收 EventLoop
    private void disposeResources() {
        try {
            if (disposableServer != null) disposableServer.disposeNow(Duration.ofSeconds(30));
        } finally {
            disposableServer = null;
            if (loopResources != null) {
                try {
                    loopResources.disposeLater()
                            .timeout(Duration.ofSeconds(10))
                            .onErrorResume(ex -> {
                                logger.error("failed to close loop resources:", ex);
                                return Mono.empty();
                            })
                            .block(Duration.ofSeconds(15));
                } finally {
                    loopResources = null;
                }
            }
        }
    }

    @Override
    public void registerHttpHandler(HttpHandler httpHandler, String handlePath) {
        registerHttpHandler(httpHandler, handlePath, false);
    }

    @Override
    public void registerHttpHandler(HttpHandler httpHandler, String handlePath, boolean noAuth, String... methods) {
        Map<String, String> properties = new HashMap<>();
        properties.put(HttpHandler.HANDLE_PATH, handlePath);
        properties.put(HttpHandler.HANDLE_PATH_ABSOLUTE, "true");
        if (noAuth) {
            properties.put(HttpHandler.HANDLE_NO_AUTH, "true");
        }
        if (methods != null && methods.length > 0) {
            properties.put(HttpHandler.HANDLE_METHODS, String.join(",", methods));
        }
        handlerManager.addHttpHandler(httpHandler, properties, null);
    }

    @Override
    public void unregisterHttpHandler(HttpHandler httpHandler) {
        handlerManager.removeHttpHandler(httpHandler);
    }
}
