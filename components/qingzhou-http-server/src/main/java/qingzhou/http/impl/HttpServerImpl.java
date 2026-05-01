package qingzhou.http.impl;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.channel.ChannelOption;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.*;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpServer;
import qingzhou.logger.Logger;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.resources.LoopResources;

@Component(immediate = true,
        configurationPid = "qingzhou-http-server", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class HttpServerImpl implements HttpServer {
    private final List<String> tempMsg = new ArrayList<>();

    @Reference
    private Logger logger;

    final Map<String, HttpHandler> handlerMap = new HashMap<>();

    private LoopResources loopResources;
    private DisposableServer disposableServer;
    private ThreadPoolExecutor taskThreadPool;

    @Activate
    public synchronized void start(Map<String, String> config) {
        int selectorThreads = getConfig(config, "selector", 1);
        int workerThreads = getConfig(config, "worker", Runtime.getRuntime().availableProcessors() * 2);
        int taskThreads = getConfig(config, "task", 50);
        int idleTimeout = getConfig(config, "idle_timeout", 60);

        String host = getConfig(config, "host", "0.0.0.0");
        int port = Integer.parseInt(config.get("port"));

        // 1. 创建可复用的 EventLoop 资源（生产必备：避免线程池重复创建，支持优雅关闭）
        loopResources = LoopResources.create(
                "http-server",  // 线程名称前缀（方便排查）
                selectorThreads,      // Boss线程数
                workerThreads,        // Worker线程数
                true                  // 是否为守护线程（生产建议true，不阻塞应用退出）
        );
        taskThreadPool = buildTaskThreadPool(taskThreads);

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
                .handle(new DispatcherHandler(this, taskThreadPool, logger));

        // 3. 启动服务并持有 Disposable（关键：用于后续优雅停止）
        disposableServer = httpServer.bindNow();

        tempMsg.forEach(s -> logger.info(s));
        logger.info("http server started: http://localhost:" + port + "/console");
    }

    private <T> T getConfig(Map<String, String> config, String key, T defaultValue) {
        String val = config.get(key);
        if (val == null || val.isEmpty()) {
            return defaultValue;
        }

        if (defaultValue instanceof Integer) {
            try {
                return (T) Integer.valueOf(val);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else if (defaultValue instanceof String) {
            return (T) val;
        } else if (defaultValue instanceof Boolean) {
            return (T) Boolean.valueOf(val);
        }
        return defaultValue;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            unbind = "removeHttpHandler")
    public synchronized void addHttpHandler(HttpHandler httpHandler, Map<String, String> properties) {
        String path = properties.get(HttpHandler.HANDLE_PATH);
        String component = properties.get(ComponentConstants.COMPONENT_NAME);
        if (component == null) component = "@App";
        if (path == null)
            throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + " of [" + component + "] cannot be null");
        path = path.trim();
        if (!path.startsWith("/"))
            throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + " of [" + component + "] must start with '/', but it currently is: " + path);

        if (handlerMap.containsKey(path)) {
            throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + "(" + path + ") of [" + component + "] already exists: " + path + " of [" + handlerMap.get(path).getClass().getName() + "]");
        } else {
            String matches = matches(path);
            if (matches != null && !matches.equals("/")) {
                throw new IllegalArgumentException(HttpHandler.HANDLE_PATH + "(" + path + ") of [" + component + "] matches: " + matches + " of [" + handlerMap.get(matches).getClass().getName() + "]");
            }
        }

        handlerMap.put(path, httpHandler);

        String msg = "http handler registered: " + path;
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
            }
        }
        handlerMap.remove(contextPath);

        logger.info("http handler unregistered: " + contextPath);
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
                    logger.error("Failed to close loop resources:", ex);
                    return Mono.empty();
                })
                .subscribe(); // 非阻塞订阅

        // 关闭业务线程池（实际业务中根据场景决定是否关闭）
        taskThreadPool.shutdown();
        try { // 等待线程池关闭（可选）
            if (!taskThreadPool.awaitTermination(5, TimeUnit.MINUTES)) {
                taskThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            taskThreadPool.shutdownNow();
        }

        logger.info("http server stopped");
    }

    private ThreadPoolExecutor buildTaskThreadPool(int maximumPoolSize) {
        int corePoolSize = maximumPoolSize / 5;          // 核心线程数（常驻）
        if (corePoolSize < 1) corePoolSize = 1;

        // 线程工厂：自定义线程名称，方便日志排查
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicLong count = new AtomicLong(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("task-" + count.incrementAndGet());
                return thread;
            }
        };

        // 创建线程池
        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(maximumPoolSize * 2), // 任务队列：有界队列（避免无界队列导致内存溢出）
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy() // 拒绝策略：任务过多时，直接抛出异常（可根据业务调整）
        );
    }

    @Override
    public void registerHttpHandler(HttpHandler httpHandler, String handlePath) {
        addHttpHandler(httpHandler, new HashMap<String, String>() {{
            put(HttpHandler.HANDLE_PATH, handlePath);
        }});
    }

    @Override
    public void unregisterHttpHandler(HttpHandler httpHandler) {
        for (Map.Entry<String, HttpHandler> entry : handlerMap.entrySet()) {
            if (entry.getValue().equals(httpHandler)) {
                String key = entry.getKey();
                handlerMap.remove(key);
                break;
            }
        }
    }
}
