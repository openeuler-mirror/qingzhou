package qingzhou.http.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.*;
import qingzhou.crypto.Crypto;
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
    private Crypto crypto;

    @Reference
    private Logger logger;

    // handler 由 OSGi 动态注册/解绑，与请求分发并发读写，故用并发容器
    final Map<String, HttpHandler> handlerMap = new ConcurrentHashMap<>();
    final Set<HttpHandler> noAuthHandlerSet = ConcurrentHashMap.newKeySet();

    // OSGi 动态绑定与请求线程并发读写，须用写时复制容器避免遍历中结构变更
    private final List<Authenticator> authenticators = new CopyOnWriteArrayList<>();

    private LoopResources loopResources;
    private DisposableServer disposableServer;
    boolean isAuthDisabled;
    boolean isSslEnabled;
    int maxConcurrentRequests;
    int maxBodyBytes; // 单个请求体聚合进内存的上限
    long maxStreamBytes; // 流式上传总量上限
    String csp;

    @Activate
    public synchronized void start(Map<String, String> config) throws Exception {
        int selectorThreads = getConfig(config, "selector", 1);
        int workerThreads = getConfig(config, "worker", Runtime.getRuntime().availableProcessors() * 2);
        // 默认 60 秒：SSE 等长连接在两个数据包之间可能长时间静默，过低会切断正常业务
        int idleTimeout = getConfig(config, "idle_timeout", 60);
        maxConcurrentRequests = getConfig(config, "max_concurrent_requests", 1000);
        if (maxConcurrentRequests <= 0) throw new IllegalArgumentException("max_concurrent_requests must be positive");
        maxBodyBytes = getConfig(config, "max_body_bytes", 8 * 1024 * 1024);
        maxStreamBytes = getConfig(config, "max_stream_bytes", 1024L * 1024 * 1024);
        csp = getConfig(config, "csp", "none");

        String host = getConfig(config, "host", "0.0.0.0");
        String portValue = config.get("port");
        if (portValue == null || portValue.trim().isEmpty()) {
            throw new IllegalArgumentException("port is required");
        }
        int port = Integer.parseInt(portValue.trim());

        // 密钥库校验必须在绑定端口前完成，任一配置错误都应直接启动失败且不监听端口
        isSslEnabled = getConfig(config, "ssl_enabled", true);
        SslContext sslContext = isSslEnabled ? buildSslContext(config) : null;

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
        tempMsg.clear();

        logger.info("http server started: " + (isSslEnabled ? "https" : "http") + "://localhost:" + port);
    }

    /**
     * 加载 SSL 密钥库并构建服务端 SslContext。
     * 任何配置缺失或错误（未配置路径、文件不存在、口令缺失、类型非法）都会在此抛出异常，
     * 使服务在绑定端口前启动失败，绝不回退为明文监听。
     */
    private SslContext buildSslContext(Map<String, String> config) {
        String keystorePath = config.get("ssl_keystore_path");
        if (keystorePath == null || keystorePath.trim().isEmpty()) {
            throw new IllegalArgumentException("ssl_keystore_path is required when ssl_enabled=true");
        }

        File keystoreFile = new File(keystorePath.trim());
        if (!keystoreFile.isFile()) {
            throw new IllegalArgumentException("ssl keystore file does not exist: " + keystoreFile
                    + ", generate it with bin/gen-keystore.sh");
        }

        String type = config.get("ssl_keystore_type");
        type = (type == null || type.trim().isEmpty()) ? "PKCS12" : type.trim().toUpperCase(Locale.ROOT);
        if (!"PKCS12".equals(type) && !"JKS".equals(type)) {
            throw new IllegalArgumentException("unsupported ssl_keystore_type: " + type + ", only PKCS12 or JKS is supported");
        }

        String password = config.get("ssl_keystore_password");
        if (password == null || password.isEmpty()) { // 口令强度策略交由部署方决定，此处只校验配置完整性
            throw new IllegalArgumentException("ssl_keystore_password is required when ssl_enabled=true"
                    + ", generate it with bin/gen-keystore.sh");
        }
        char[] keyPassword = crypto.getGlobalCipher().tryDecrypt(password, "ssl_keystore_password").toCharArray();

        try (InputStream in = Files.newInputStream(keystoreFile.toPath())) {
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(in, keyPassword);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPassword);
            // 显式收敛协议：不指定时继承 JDK 默认，部分环境仍会启用 TLSv1.0/1.1
            return SslContextBuilder.forServer(keyManagerFactory).protocols(tlsProtocols()).build();
        } catch (Exception e) {
            throw new IllegalStateException("failed to load ssl keystore: " + keystoreFile, e);
        } finally {
            Arrays.fill(keyPassword, '\0');
        }
    }

    // TLSv1.3 需要 JDK 11+，不可用时退到 TLSv1.2
    private static String[] tlsProtocols() {
        try {
            for (String protocol : SSLContext.getDefault().getSupportedSSLParameters().getProtocols()) {
                if ("TLSv1.3".equals(protocol)) return new String[]{"TLSv1.3", "TLSv1.2"};
            }
        } catch (NoSuchAlgorithmException e) {
            // 取不到支持列表时按最保守的 TLSv1.2 处理
        }
        return new String[]{"TLSv1.2"};
    }

    private <T> T getConfig(Map<String, String> config, String key, T defaultValue) {
        String val = config.get(key);
        if (val == null || val.isEmpty()) return defaultValue;

        if (defaultValue instanceof String) return (T) val;
        // 数值解析失败直接抛出，暴露配置笔误而非静默回退默认值
        if (defaultValue instanceof Integer) return (T) Integer.valueOf(val);
        if (defaultValue instanceof Long) return (T) Long.valueOf(val);
        if (defaultValue instanceof Boolean) return (T) Boolean.valueOf(val);

        return defaultValue;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            unbind = "removeHttpHandler")
    public synchronized void addHttpHandler(HttpHandler httpHandler, Map<String, String> properties, ServiceReference<HttpHandler> reference) {
        String originPath = properties.get(HttpHandler.HANDLE_PATH);
        String component = properties.get(ComponentConstants.COMPONENT_NAME);
        if (component == null) component = "@App";
        if (originPath == null || originPath.trim().isEmpty()) {
            throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + " of [" + component + "] cannot be empty");
        }

        String path = originPath.trim();
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + " of [" + component + "] must start with /");
        }
        if (reference != null) {
            String prefix = reference.getBundle().getSymbolicName();
            prefix = prefix.replace("qingzhou-", "");
            path = "/" + prefix + path;
        }
        path = withTrailingSlash(path); // 统一以尾斜杠存储，匹配时无需逐个 key 再做转换

        String conflict = conflict(path);
        if (conflict != null) {
            throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + "(" + originPath + ") of [" + component + "] conflicts: " + conflict + " of [" + handlerMap.get(conflict).getClass().getName() + "]");
        }

        handlerMap.put(path, httpHandler);
        boolean isNoAuth = Boolean.parseBoolean(properties.get(HttpHandler.HANDLE_NO_AUTH));
        if (isNoAuth) {
            noAuthHandlerSet.add(httpHandler);
        }

        // 在 ReferencePolicy.DYNAMIC 内，Logger 可能尚未注入，故先暂存消息，在 @Activate 中一起输出
        String msg = "http handler registered, component: " + component + ", path: " + originPath + (isNoAuth ? " (no auth)" : "");
        if (logger != null) {
            logger.info(msg);
        } else {
            tempMsg.add(msg);
        }
    }

    /**
     * 分发专用：只按「请求路径以已注册路径为前缀」匹配，并取最长者。
     * 反向匹配（已注册路径以请求路径为前缀）会让后代 handler 服务祖先请求，
     * 例如请求 / 命中 /ai/chat/config、请求 /ai/chat 命中 /ai/chat/stream，故不做。
     * 直接返回 handler 而非路径：OSGi 可并发解绑 handler，先查路径再取 handler 会取到 null。
     */
    HttpHandler findHandler(String checkPath) {
        String request = withTrailingSlash(checkPath);
        HttpHandler matched = null;
        int matchedLength = 0;
        for (Map.Entry<String, HttpHandler> entry : handlerMap.entrySet()) {
            String existsPath = entry.getKey();
            if (existsPath.length() > matchedLength && request.startsWith(existsPath)) {
                matched = entry.getValue();
                matchedLength = existsPath.length();
            }
        }
        return matched;
    }

    // 注册专用：父子路径任一方向重叠即冲突，避免二者在分发时互相遮蔽
    private String conflict(String checkPath) {
        String request = withTrailingSlash(checkPath);
        for (String existsPath : handlerMap.keySet()) {
            if (existsPath.equals("/")) continue; // 根路径仅作兜底（它必然与所有路径重叠）
            if (request.startsWith(existsPath) || existsPath.startsWith(request)) return existsPath;
        }
        return null;
    }

    // 补尾部斜杠：使 /a 只匹配 /a/...，不会误匹配 /abc
    private static String withTrailingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    /**
     * 解绑方法的名称由被注解方法的名称生成。
     * 如果被注解方法的名称以bind、set或add开头，则会分别将这些前缀替换为unbind、unset或remove，以此生成解绑方法的候选名称；
     * 若被注解方法的名称不以这些前缀开头，则会在方法名前添加前缀un，生成解绑方法的候选名称。
     * 若组件类中存在一个方法与该候选名称一致，则此候选名称即作为解绑方法的名称。
     * 若组件类中存在该候选名称对应的方法，但开发者希望不声明任何解绑方法，则必须将该属性值设为-。
     */
    public synchronized void removeHttpHandler(HttpHandler httpHandler) {
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

        logger.info("http handler unregistered: " + contextPath);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            unbind = "removeAuthenticator")
    public synchronized void addAuthenticator(Authenticator authenticator) {
        authenticators.add(authenticator);

        // 在 ReferencePolicy.DYNAMIC 内，Logger 可能尚未注入，故先暂存消息，在 @Activate 中一起输出
        String msg = "http authenticator registered: " + authenticator.getClass().getName();
        if (logger != null) {
            logger.info(msg);
        } else {
            tempMsg.add(msg);
        }
    }

    public synchronized void removeAuthenticator(Authenticator authenticator) {
        authenticators.remove(authenticator);
    }

    /**
     * 安全认证：配置 auth_disabled=true 时全局关闭；多认证器按 pass > reject > challenge > missing 组合——
     * 任一通过即放行；凭据无效优先拒绝（客户端已出示凭据，须明确告知 401 而非重定向）；
     * 全部无凭据时才用重定向引导登录。
     */
    AuthResult authenticate(HttpRequest request) {
        if (authenticators.isEmpty()) return AuthResult.reject("no authenticator ready");

        AuthResult reject = null;
        for (Authenticator authenticator : authenticators) {
            AuthResult r;
            try {
                r = authenticator.authenticate(request);
            } catch (Exception e) {
                logger.error("authentication error: " + authenticator.getClass().getName(), e);
                r = AuthResult.reject("authentication error");
            }
            if (r.status() == Status.PASS) return r;

            if (r.status() == Status.REJECT && reject == null) {
                reject = r;
            }
        }
        if (reject != null) return reject;

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
        addHttpHandler(httpHandler, properties, null);
    }

    @Override
    public void unregisterHttpHandler(HttpHandler httpHandler) {
        removeHttpHandler(httpHandler);
    }
}
