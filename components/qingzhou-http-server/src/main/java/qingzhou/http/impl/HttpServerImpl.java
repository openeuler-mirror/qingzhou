package qingzhou.http.impl;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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
        int selectorThreads = getConfig(config, "selector", 1);
        int workerThreads = getConfig(config, "worker", Runtime.getRuntime().availableProcessors() * 2);
        int idleTimeout = getConfig(config, "idle_timeout", 60); // 默认 60 秒：SSE 等长连接在两个数据包之间可能长时间静默，过低会切断正常业务
        String host = getConfig(config, "host", "0.0.0.0");
        String portStr = config.get("port");
        if (portStr == null || portStr.trim().isEmpty()) {
            throw new IllegalArgumentException("port is required");
        }
        int port = Integer.parseInt(portStr.trim());

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
                // TCP 底层配置（生产环境优化必备，防止半连接、粘包等问题）
                .option(ChannelOption.SO_REUSEADDR, true) // tcp 层端口复用（高效，但有数据混乱低风险，因netty等框架有容错检测故可打开）
                .option(ChannelOption.SO_BACKLOG, 1024) // tcp 层连接队列，应对突发流量避免客户端被拒绝，过大会消耗系统资源
                .childOption(ChannelOption.SO_KEEPALIVE, true) // tcp 层保活探测，避免对方意外断电等资源无效占用
                .childOption(ChannelOption.TCP_NODELAY, true) // 现代带宽充足，路由器处理能力强，「小包风暴」的影响远小于实时性不足带来的业务问题
                .idleTimeout(Duration.ofSeconds(idleTimeout)) // 一条连接，无任何读或写活动，则主动关闭连接释放资源，不设置则无限
                // 业务路由（生产环境建议抽离到单独的 Handler 类，解耦业务逻辑）
                .handle(dispatcherHandler);

        // 密钥库校验必须在绑定端口前完成，任一配置错误都应直接启动失败且不监听端口
        boolean isSslEnabled = getConfig(config, "ssl_enabled", true);
        if (isSslEnabled) {
            SslContext sslContext = SslContextBuilder.buildSslContext(config, crypto.getGlobalCipher());
            httpServer = httpServer.secure(spec -> spec.sslContext(sslContext));
        }

        // 启动服务并持有 Disposable（关键：用于后续优雅停止）
        disposableServer = httpServer.bindNow();

        logger.info("http server started: " + (isSslEnabled ? "https" : "http") + "://localhost:" + port + "/web");
    }

    @Deactivate
    public void stop() { // 与 start() 同锁，避免 SCR 重配置交错时误关新实例
        if (disposableServer == null) return;

        // 优雅关闭HTTP服务（超时30秒）
        disposableServer.disposeNow(Duration.ofSeconds(30));

        // 关闭 EventLoop 资源
        loopResources.disposeLater()
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(ex -> {
                    logger.error("failed to close loop resources:", ex);
                    return Mono.empty();
                })
                .subscribe(); // 非阻塞订阅

        logger.info("http server stopped");
    }

    @Override
    public void registerHttpHandler(HttpHandler httpHandler, String handlePath) {
        register(httpHandler, handlePath, false);
    }

    @Override
    public void registerHttpHandlerNoAuth(HttpHandler httpHandler, String handlePath) {
        register(httpHandler, handlePath, true);
    }

    // 两个注册 API 仅差一个 no-auth 属性，收敛到同一构建逻辑
    private void register(HttpHandler httpHandler, String handlePath, boolean noAuth) {
        Map<String, String> properties = new HashMap<>();
        properties.put(HttpHandler.HANDLE_PATH, handlePath);
        if (noAuth) properties.put(HttpHandler.HANDLE_NO_AUTH, "true");
        handlerManager.addHttpHandler(httpHandler, properties, null);
    }

    @Override
    public void unregisterHttpHandler(HttpHandler httpHandler) {
        handlerManager.removeHttpHandler(httpHandler);
    }

    static <T> T getConfig(Map<String, String> config, String key, T defaultValue) {
        String val = config.get(key);
        if (val == null || val.isEmpty()) return defaultValue;

        if (defaultValue instanceof String) return (T) val;
        // 数值解析失败直接抛出，暴露配置笔误而非静默回退默认值
        if (defaultValue instanceof Integer) return (T) Integer.valueOf(val);
        if (defaultValue instanceof Long) return (T) Long.valueOf(val);
        if (defaultValue instanceof Boolean) return (T) Boolean.valueOf(val);

        return defaultValue;
    }
}
