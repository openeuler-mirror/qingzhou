package qingzhou.http.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.time.Duration;
import java.util.*;

import javax.net.ssl.KeyManagerFactory;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.*;
import qingzhou.http.server.*;
import qingzhou.http.server.AuthResult.Status;
import qingzhou.logger.Logger;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.resources.LoopResources;

@Component(immediate = true, configurationPid = "qingzhou-http-server", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class HttpServerImpl implements HttpServer {
    private final List<String> tempMsg = new ArrayList<>();

    @Reference
    private Logger logger;

    final Map<String, HttpHandler> handlerMap = new HashMap<>();
    final Set<HttpHandler> noAuthHandlerSet = new HashSet<>();

    private final List<HttpAuthenticator> authenticators = new ArrayList<>();

    private LoopResources loopResources;
    private DisposableServer disposableServer;
    boolean isAuthDisabled;

    @Activate
    public synchronized void start(Map<String, String> config) {
        int selectorThreads = getConfig(config, "selector", 1);
        int workerThreads = getConfig(config, "worker", Runtime.getRuntime().availableProcessors() * 2);
        int idleTimeout = getConfig(config, "idle_timeout", 60);

        String host = getConfig(config, "host", "0.0.0.0");
        int port = Integer.parseInt(config.get("port"));

        boolean sslEnabled = getConfig(config, "ssl_enabled", false);
        // 密钥库校验必须在绑定端口前完成，任一配置错误都应直接启动失败且不监听端口
        SslContext sslContext = sslEnabled ? buildSslContext(config) : null;

        isAuthDisabled = getConfig(config, "auth_disabled", false);
        if (isAuthDisabled) logger.warn("http server authentication is disabled");

        // 1. 创建可复用的 EventLoop 资源（生产必备：避免线程池重复创建，支持优雅关闭）
        loopResources = LoopResources.create(
                "http-server",  // 线程名称前缀（方便排查）
                selectorThreads,      // Boss线程数
                workerThreads,        // Worker线程数
                true                  // 是否为守护线程（生产建议true，不阻塞应用退出）
        );

        // 2. 构建生产级 HTTP 服务（配置超时、线程池、TCP 选项）
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
                .handle(new DispatcherHandler(this, logger));

        // https 模式：HTTP 与 HTTPS 二选一，不提供明文兜底监听
        if (sslContext != null) {
            httpServer = httpServer.secure(spec -> spec.sslContext(sslContext));
        }

        // 3. 启动服务并持有 Disposable（关键：用于后续优雅停止）
        disposableServer = httpServer.bindNow();

        tempMsg.forEach(s -> logger.info(s));
        logger.info("http server started: " + (sslEnabled ? "https" : "http") + "://localhost:" + port + "/web");
    }

    /**
     * 加载 SSL 密钥库并构建服务端 SslContext。
     * 任何配置缺失或错误（未配置路径、文件不存在、口令错误、类型非法）都会在此抛出异常，
     * 使服务在绑定端口前启动失败，绝不回退为明文监听。
     */
    private static SslContext buildSslContext(Map<String, String> config) {
        String keystorePath = config.get("ssl_keystore_path");
        if (keystorePath == null || keystorePath.trim().isEmpty()) {
            throw new IllegalArgumentException("ssl_keystore_path is required when ssl_enabled=true");
        }

        File keystoreFile = new File(keystorePath.trim());
        if (!keystoreFile.isFile()) {
            throw new IllegalArgumentException("ssl keystore file does not exist: " + keystoreFile);
        }

        String type = config.get("ssl_keystore_type");
        type = (type == null || type.trim().isEmpty()) ? "PKCS12" : type.trim().toUpperCase(Locale.ROOT);
        if (!"PKCS12".equals(type) && !"JKS".equals(type)) {
            throw new IllegalArgumentException("unsupported ssl_keystore_type: " + type + ", only PKCS12 or JKS is supported");
        }

        String password = config.get("ssl_keystore_password");
        char[] keyPassword = password == null ? new char[0] : password.toCharArray();

        try (InputStream in = new FileInputStream(keystoreFile)) {
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(in, keyPassword);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPassword);
            return SslContextBuilder.forServer(keyManagerFactory).build();
        } catch (Exception e) {
            throw new IllegalStateException("failed to load ssl keystore: " + keystoreFile, e);
        }
    }

    private <T> T getConfig(Map<String, String> config, String key, T defaultValue) {
        String val = config.get(key);
        if (val == null || val.isEmpty()) return defaultValue;

        if (defaultValue instanceof String) return (T) val;

        if (defaultValue instanceof Integer) {
            try {
                return (T) Integer.valueOf(val);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        if (defaultValue instanceof Long) {
            try {
                return (T) Long.valueOf(val);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        if (defaultValue instanceof Boolean) {
            return (T) Boolean.valueOf(val);
        }

        return defaultValue;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            unbind = "removeHttpHandler")
    public synchronized void addHttpHandler(HttpHandler httpHandler, Map<String, String> properties, ServiceReference<HttpHandler> reference) {
        String path = properties.get(HttpHandler.HANDLE_PATH);
        String component = properties.get(ComponentConstants.COMPONENT_NAME);
        if (component == null) component = "@App";
        if (path == null)
            throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + " of [" + component + "] cannot be null");
        path = path.trim();

        if (reference != null) {
            String prefix = reference.getBundle().getSymbolicName();
            prefix = prefix.replace("qingzhou-", "");
            path = "/" + prefix + path;
        }

        if (handlerMap.containsKey(path)) {
            throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + "(" + path + ") of [" + component + "] already exists: " + path + " of [" + handlerMap.get(path).getClass().getName() + "]");
        } else {
            String matches = matches(path);
            if (matches != null && !matches.equals("/")) {
                throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + "(" + path + ") of [" + component + "] matches: " + matches + " of [" + handlerMap.get(matches).getClass().getName() + "]");
            }
        }

        handlerMap.put(path, httpHandler);
        boolean isNoAuth = Boolean.parseBoolean(properties.get(HttpHandler.HANDLE_NO_AUTH));
        if (isNoAuth) {
            noAuthHandlerSet.add(httpHandler);
        }

        String msg = "http handler registered: " + path + (isNoAuth ? " (no auth)" : "");
        if (logger != null) { // osgi ds 尚未规范：AppStubLocal 的注入 可能早于 logger
            logger.info(msg);
        } else {
            tempMsg.add(msg);
        }
    }

    String matches(String checkPath) {
        // 排序：长路径优先匹配（避免短路径覆盖长路径）
        List<String> existsPaths = new ArrayList<>(handlerMap.keySet());
        existsPaths.sort((a, b) -> b.length() - a.length());

        if (!checkPath.endsWith("/")) checkPath = checkPath + "/";
        for (String existsPath : existsPaths) {
            String tempPath = existsPath;
            if (!tempPath.endsWith("/")) tempPath = tempPath + "/";
            if (checkPath.startsWith(tempPath)
                    || tempPath.startsWith(checkPath)) {
                return existsPath;
            }
        }
        return null;
    }

    /**
     * 解绑方法的名称由被注解方法的名称生成。
     * 如果被注解方法的名称以bind、set或add开头，则会分别将这些前缀替换为unbind、unset或remove，以此生成解绑方法的候选名称；
     * 若被注解方法的名称不以这些前缀开头，则会在方法名前添加前缀un，生成解绑方法的候选名称。
     * 若组件类中存在一个方法与该候选名称一致，则此候选名称即作为解绑方法的名称。
     * 若组件类中存在该候选名称对应的方法，但开发者希望不声明任何解绑方法，则必须将该属性值设为-。
     */
    public void removeHttpHandler(HttpHandler httpHandler) {
        String contextPath = null;
        for (Map.Entry<String, HttpHandler> e : handlerMap.entrySet()) {
            if (Objects.equals(e.getValue(), httpHandler)) {
                contextPath = e.getKey();
                break;
            }
        }
        if (contextPath == null) return;

        handlerMap.remove(contextPath);
        noAuthHandlerSet.remove(httpHandler);

        if (logger != null) { // osgi ds 尚未规范：解绑可能早于 logger 注入
            logger.info("http handler unregistered: " + contextPath);
        }
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            unbind = "removeAuthentication")
    public void addAuthentication(HttpAuthenticator authenticator) {
        authenticators.add(authenticator);

        if (logger != null) { // osgi ds 尚未规范：认证器可能早于 logger 注入
            logger.info("http authenticator registered: " + authenticator.getClass().getName());
        }
    }

    public void removeAuthentication(HttpAuthenticator authentication) {
        authenticators.remove(authentication);
    }

    /**
     * 安全认证：配置 auth_disabled=true 时全局关闭；命中认证器声明的豁免路径放行；多认证器按 pass > reject > challenge > missing 组合——
     * 任一通过即放行；凭据无效优先拒绝（客户端已出示凭据，须明确告知 401 而非重定向）；
     * 全部无凭据时才用重定向引导登录。
     */
    AuthResult authenticate(HttpRequest request) {
        if (authenticators.isEmpty()) return AuthResult.reject("no authenticator ready");

        String path = request.getPath();
        for (HttpAuthenticator authenticator : authenticators) {
            String[] excludedPaths = authenticator.excludedPaths();
            if (excludedPaths != null) {
                for (String exclude : excludedPaths) {
                    if (path.startsWith(exclude)) return AuthResult.pass(null);
                }
            }
        }

        AuthResult reject = null;
        AuthResult challenge = null;
        for (HttpAuthenticator authentication : authenticators) {
            AuthResult r;
            try {
                r = authentication.authenticate(request);
            } catch (Exception e) {
                logger.error("authentication error: " + authentication.getClass().getName(), e);
                r = AuthResult.reject("authentication error");
            }
            if (r.status() == Status.PASS) return r;

            if (r.status() == Status.REJECT && reject == null) {
                reject = r;
            } else if (r.status() == Status.CHALLENGE && challenge == null) {
                challenge = r;
            }
        }
        if (reject != null) return reject;
        if (challenge != null) return challenge;

        return AuthResult.reject("no credential provided");
    }

    @Deactivate
    public void stop() {
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
        addHttpHandler(httpHandler, new HashMap<String, String>() {{
            put(HttpHandler.HANDLE_PATH, handlePath);
        }}, null);
    }

    @Override
    public void registerHttpHandlerNoAuth(HttpHandler httpHandler, String handlePath) {
        addHttpHandler(httpHandler, new HashMap<String, String>() {{
            put(HttpHandler.HANDLE_PATH, handlePath);
            put(HttpHandler.HANDLE_NO_AUTH, "true");
        }}, null);
    }

    @Override
    public void unregisterHttpHandler(HttpHandler httpHandler) {
        removeHttpHandler(httpHandler);
    }
}
