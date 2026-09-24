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
        int selectorThreads = atLeast(getConfig(config, "selector", 1), 1, "selector");
        int workerThreads = atLeast(getConfig(config, "worker", Runtime.getRuntime().availableProcessors() * 2), 1, "worker");
        // 默认 60 秒：SSE 等长连接在两个数据包之间可能长时间静默，过低会切断正常业务；0 表示不启用空闲超时
        int idleTimeout = atLeast(getConfig(config, "idle_timeout", 60), 0, "idle_timeout");
        String host = getConfig(config, "host", "0.0.0.0");
        String portStr = config.get("port");
        if (portStr == null || portStr.trim().isEmpty()) {
            throw new IllegalArgumentException("port is required");
        }
        int port = Integer.parseInt(portStr.trim());
        // 0 是合法值：交由操作系统分配空闲端口（测试与动态端口场景依赖它）
        if (port < 0 || port > 65535)
            throw new IllegalArgumentException("qingzhou-http-server.port out of range: " + port);

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
                .httpRequestDecoder(spec -> spec.maxInitialLineLength(4096).maxHeaderSize(8192)) // 显式收敛请求行/头，不依赖框架默认值（升级即行为漂移）
                .handle(dispatcherHandler); // 业务路由

        // 一条连接无任何读写活动则关闭释放资源；0 表示不启用，留给 SSE 等长连接
        if (idleTimeout > 0) {
            httpServer = httpServer.idleTimeout(Duration.ofSeconds(idleTimeout));
        }

        // 密钥库校验必须在绑定端口前完成，任一配置错误都应直接启动失败且不监听端口
        boolean isSslEnabled = getConfig(config, "ssl_enabled", true);
        try {
            if (isSslEnabled) {
                SslContext sslContext = SslContextBuilder.buildSslContext(config, crypto.getGlobalCipher(), logger);
                httpServer = httpServer.secure(spec -> spec.sslContext(sslContext));
            }
            disposableServer = httpServer.bindNow();
        } catch (Exception e) {
            disposeResources(); // 激活失败时 @Deactivate 不会被调用，已创建的 EventLoop 须在此回收
            throw e;
        }

        logger.info("http server started: " + (isSslEnabled ? "https" : "http") + "://localhost:" + port + "/web");
    }

    @Deactivate
    public void stop() {
        disposeResources();
        logger.info("http server stopped");
    }

    // 置空已释放的资源：使 stop 可重复调用，且 start 失败路径可安全复用
    private void disposeResources() {
        if (disposableServer != null) {
            disposableServer.disposeNow(Duration.ofSeconds(30)); // 优雅关闭，超时 30 秒
            disposableServer = null;
        }
        if (loopResources != null) {
            loopResources.disposeLater()
                    .timeout(Duration.ofSeconds(10))
                    .onErrorResume(ex -> {
                        logger.error("failed to close loop resources:", ex);
                        return Mono.empty();
                    })
                    .block(Duration.ofSeconds(15)); // 同步等待：否则 stop 返回后线程仍在运行，重启时端口与线程残留
            loopResources = null;
        }
    }

    // 配置值下限校验：min=1 表示必须为正，min=0 表示可为 0 但不能为负
    static int atLeast(int value, int min, String key) {
        if (value < min)
            throw new IllegalArgumentException("qingzhou-http-server." + key + " must be >= " + min + ": " + value);
        return value;
    }

    static long atLeast(long value, long min, String key) {
        if (value < min)
            throw new IllegalArgumentException("qingzhou-http-server." + key + " must be >= " + min + ": " + value);
        return value;
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

    @SuppressWarnings("unchecked") // 按 defaultValue 的实际类型分派，四种分支各自类型安全
    static <T> T getConfig(Map<String, String> config, String key, T defaultValue) {
        String val = config.get(key);
        if (val == null) return defaultValue;
        val = val.trim(); // 值允许带前后空白，但不得因此解析失败或静默降级
        if (val.isEmpty()) return defaultValue;

        if (defaultValue instanceof String) return (T) val;
        // 数值解析失败直接抛出，暴露配置笔误而非静默回退默认值
        if (defaultValue instanceof Integer) return (T) Integer.valueOf(val);
        if (defaultValue instanceof Long) return (T) Long.valueOf(val);
        if (defaultValue instanceof Boolean) {
            // 布尔不得静默降级：非 true/false 一律报错，否则 ssl_enabled=TRUE/1 会被解析为关闭
            if ("true".equals(val) || "false".equals(val)) return (T) Boolean.valueOf(val);
            throw new IllegalArgumentException("qingzhou-http-server." + key + " must be exactly 'true' or 'false' (case sensitive), got: " + val);
        }

        return defaultValue;
    }
}
