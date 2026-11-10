package qingzhou.monitor.prometheus;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.component.annotations.*;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.logger.Logger;
import qingzhou.monitor.Metric;
import qingzhou.monitor.MetricProvider;
import qingzhou.monitor.collector.MonitorCollector;
import qingzhou.registry.AppStub;
import qingzhou.registry.Constants;
import qingzhou.registry.Registry;

/**
 * 监控抓取端点：应用字段、MetricProvider、端点自监控统一走 Metric + MetricFormat 渲染。
 * 采集含同步远程 RPC，故声明 async 并派发到单业务线程；任一 target 失败只影响该 target
 * （target_up=0），不再让整次抓取返回 500。
 */
@Component(configurationPid = "qingzhou-monitor", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {HttpHandler.HANDLE_PATH + "=/prometheus"})
public class PrometheusEndpoint implements HttpHandler {
    @Reference
    private MonitorCollector monitorCollector;

    /**
     * 抓取路径：绝对路径注册，不依赖 bundle 符号名推导
     */
    public static final String CONFIG_SCRAPE_INTERVAL_SECONDS = "scrape_interval_seconds";
    public static final String CONFIG_SCRAPE_DEADLINE_MS = "scrape_deadline_ms";
    public static final String PARAM_APP = "app";
    public static final String PARAM_INSTANCE = "instance";
    public static final String PARAM_PROVIDER = "provider";

    private static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    private static final long DEFAULT_SCRAPE_INTERVAL_SECONDS = 5L;
    private static final long DEFAULT_SCRAPE_DEADLINE_MS = 10_000L;
    // 抓取串行且带缓存，队列只需容纳瞬时并发；满了直接 503，避免无界堆积
    private static final int QUEUE_CAPACITY = 4;
    // 活跃令牌种类有限；超上限时只清过期项，不无限增长
    private static final int MAX_CACHE_ENTRIES = 8;
    private static final String KIND_APP = "app";
    private static final String KIND_PROVIDER = "provider";

    @Reference
    private Registry registry;
    @Reference
    private Logger logger;

    private final Map<MetricProvider, String> providers = new ConcurrentHashMap<>();
    private final AtomicLong providersVersion = new AtomicLong();
    private final Map<String, Cache> caches = new ConcurrentHashMap<>();
    // 抓取在单业务线程执行，累计量只在该线程读写
    private final Map<DropCounter.Reason, Long> droppedTotals = new EnumMap<>(DropCounter.Reason.class);

    private volatile long scrapeIntervalMs = TimeUnit.SECONDS.toMillis(DEFAULT_SCRAPE_INTERVAL_SECONDS);
    private volatile long scrapeDeadlineMs = DEFAULT_SCRAPE_DEADLINE_MS;
    private ExecutorService workers;

    @Activate
    public void init(Map<String, String> config) {
        Map<String, String> settings = config == null ? Collections.<String, String>emptyMap() : config;
        scrapeIntervalMs = TimeUnit.SECONDS.toMillis(
                parseConfig(settings, CONFIG_SCRAPE_INTERVAL_SECONDS, DEFAULT_SCRAPE_INTERVAL_SECONDS, 1, 600));
        scrapeDeadlineMs = parseConfig(settings, CONFIG_SCRAPE_DEADLINE_MS, DEFAULT_SCRAPE_DEADLINE_MS, 1_000, 60_000);
        workers = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                runnable -> {
                    Thread thread = new Thread(runnable, "qingzhou-monitor-scrape");
                    thread.setDaemon(true);
                    return thread;
                });
    }

    @Deactivate
    public void destroy() {
        if (workers != null) workers.shutdownNow();
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void bindMetricProvider(MetricProvider provider, Map<String, Object> properties) {
        // 契约适配：OSGi 组件属性值为 Object，须显式校验类型
        Object raw = properties.get(MetricProvider.PROVIDER_ID);
        if (!(raw instanceof String) || ((String) raw).trim().isEmpty()) {
            throw new IllegalArgumentException(MetricProvider.PROVIDER_ID + " must be a non-empty String: "
                    + provider.getClass().getName());
        }
        String id = ((String) raw).trim();
        if (MetricFormat.SELF_NAMESPACE.equals(MetricFormat.canonicalName(id))) {
            throw new IllegalArgumentException("provider id is reserved: " + id);
        }
        // 同名提供方会产生重复序列，Prometheus 会拒绝整次抓取，故在绑定期拒绝
        if (providers.containsValue(id)) {
            throw new IllegalArgumentException("duplicated provider id: " + id);
        }
        providers.put(provider, id);
        providersVersion.incrementAndGet();
    }

    protected void unbindMetricProvider(MetricProvider provider) {
        if (providers.remove(provider) != null) providersVersion.incrementAndGet();
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        ScrapeQuery query = new ScrapeQuery(httpRequest.getParameter(PARAM_APP),
                httpRequest.getParameter(PARAM_INSTANCE), httpRequest.getParameter(PARAM_PROVIDER),
                httpRequest.getRoles());
        httpResponse.async(); // 须在派发业务线程前声明，否则框架会提前收尾响应
        try {
            workers.execute(() -> respond(query, httpResponse));
        } catch (RejectedExecutionException e) {
            logger.warn("monitor scrape rejected: queue is full");
            try {
                httpResponse.status(503).send("scrape busy");
            } finally {
                httpResponse.finish();
            }
        }
    }

    // 异步响应必须由业务线程收尾；handler 已返回，异常无法再上抛框架，只能落日志并回 500
    private void respond(ScrapeQuery query, HttpResponse response) {
        try {
            response.contentType(CONTENT_TYPE).send(body(query));
        } catch (Throwable t) {
            if (t instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.error("monitor scrape failed", t);
            try {
                response.status(500).send("scrape failed");
            } catch (Throwable ignored) {
                // 响应已提交时只能收尾
            }
        } finally {
            response.finish();
        }
    }

    private String body(ScrapeQuery query) {
        Cache cache = cache(query);
        List<Sample> samples = new ArrayList<>();
        for (Sample sample : cache.metrics) {
            if (matches(query, sample)) samples.add(sample);
        }
        int collected = samples.size();
        for (Sample sample : selfMetrics(cache, collected)) {
            if (matches(query, sample)) samples.add(sample);
        }
        return MetricFormat.render(samples);
    }

    private Cache cache(ScrapeQuery query) {
        long now = System.currentTimeMillis();
        long registryVersion = registry.getRegistryDataVersion();
        long providerVersion = providersVersion.get();
        String key = query.rolesKey;
        Cache cached = caches.get(key);
        if (cached != null && cached.registryVersion == registryVersion && cached.providerVersion == providerVersion
                && now - cached.collectedAtMillis < scrapeIntervalMs) {
            return cached;
        }

        Cache fresh = scrape(query.roles, registryVersion, providerVersion);
        if (caches.size() >= MAX_CACHE_ENTRIES) {
            caches.entrySet().removeIf(entry -> now - entry.getValue().collectedAtMillis >= scrapeIntervalMs);
        }
        if (caches.size() < MAX_CACHE_ENTRIES) caches.put(key, fresh);
        return fresh;
    }

    private Cache scrape(String[] roles, long registryVersion, long providerVersion) {
        long start = System.currentTimeMillis();
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(scrapeDeadlineMs);
        MetricBuffer buffer = new MetricBuffer();
        DropCounter drops = new DropCounter();
        List<Target> targets = new ArrayList<>();

        int skipped = 0;
        for (String appCode : registry.getAllLocalApps()) {
            if (!collectApp(Constants.LOCAL_INSTANCE_ID, appCode, roles, buffer, targets, drops, deadlineNanos))
                skipped++;
        }
        for (String instanceId : registry.getAllRemoteInstances()) {
            List<String> remoteApps = registry.getAllRemoteApps(instanceId);
            if (remoteApps == null) continue; // 契约适配：实例可能刚被移除
            for (String appCode : remoteApps) {
                if (!collectApp(instanceId, appCode, roles, buffer, targets, drops, deadlineNanos)) skipped++;
            }
        }
        skipped += collectProviders(buffer, targets, drops, deadlineNanos);

        if (drops.total() > 0) {
            logger.warn("monitor dropped " + drops.total() + " samples (" + drops.describe()
                    + "), first: " + drops.first());
            for (Map.Entry<DropCounter.Reason, Long> entry : drops.counts().entrySet()) {
                Long total = droppedTotals.get(entry.getKey());
                droppedTotals.put(entry.getKey(), (total == null ? 0 : total) + entry.getValue());
            }
        }
        if (skipped > 0) {
            logger.warn("monitor scrape deadline exceeded, skipped targets: " + skipped);
        }
        return new Cache(buffer.metrics(), System.currentTimeMillis(), System.currentTimeMillis() - start,
                registryVersion, providerVersion, targets);
    }

    /**
     * @return false 表示因抓取超时被跳过
     */
    private boolean collectApp(String instanceId, String appCode, String[] roles, MetricBuffer buffer,
                               List<Target> targets, DropCounter drops, long deadlineNanos) {
        AppStub stub = registry.getAppStub(instanceId, appCode);
        if (stub == null) return true; // 契约适配：应用可能刚下线，不计为失败
        if (expired(deadlineNanos)) {
            targets.add(new Target(KIND_APP, instanceId, appCode, false));
            return false;
        }

        boolean up = false;
        try {
            for (Sample sample : monitorCollector.collect(instanceId, appCode, stub, roles, drops)) {
                buffer.add(sample, drops);
            }
            up = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 停机中断：恢复中断位后退出本次抓取
            throw new IllegalStateException("monitor scrape interrupted", e);
        } catch (Throwable t) {
            // 隔离：单应用失败只影响自身指标可见性
            logger.warn("monitor target failed: app=" + appCode + ", instance=" + instanceId, t);
        } finally {
            targets.add(new Target(KIND_APP, instanceId, appCode, up));
        }
        return true;
    }

    /**
     * @return 因抓取超时被跳过的提供方数量
     */
    private int collectProviders(MetricBuffer buffer, List<Target> targets, DropCounter drops, long deadlineNanos) {
        List<Map.Entry<MetricProvider, String>> entries = new ArrayList<>(providers.entrySet());
        entries.sort((left, right) -> left.getValue().compareTo(right.getValue())); // 输出顺序稳定
        int skipped = 0;
        for (Map.Entry<MetricProvider, String> entry : entries) {
            String providerId = entry.getValue();
            if (expired(deadlineNanos)) {
                targets.add(new Target(KIND_PROVIDER, Constants.LOCAL_INSTANCE_ID, providerId, false));
                skipped++;
                continue;
            }

            boolean up = false;
            try {
                List<Metric> metrics = entry.getKey().collectMetrics();
                Map<String, String> builtin = new LinkedHashMap<>();
                builtin.put("qingzhou_instance", Constants.LOCAL_INSTANCE_ID);
                builtin.put("provider", providerId);
                for (Metric metric : metrics) {
                    if (!Collections.disjoint(metric.labels().keySet(), MetricFormat.RESERVED_LABELS)) {
                        drops.add(DropCounter.Reason.RESERVED_LABEL,
                                "provider=" + providerId + ", metric=" + metric.name());
                        continue;
                    }
                    buffer.add(Sample.of(metric, providerId + "_" + metric.name(), builtin), drops);
                }
                up = true;
            } catch (Throwable t) {
                logger.warn("monitor target failed: provider=" + providerId, t);
            } finally {
                targets.add(new Target(KIND_PROVIDER, Constants.LOCAL_INSTANCE_ID, providerId, up));
            }
        }
        return skipped;
    }

    private List<Sample> selfMetrics(Cache snapshot, int samples) {
        Map<String, String> labels = Collections.singletonMap("qingzhou_instance", Constants.LOCAL_INSTANCE_ID);
        List<Sample> metrics = new ArrayList<>();
        metrics.add(new Sample("monitor_scrape_duration_seconds", snapshot.durationMs / 1000d, Metric.Type.GAUGE, labels));
        metrics.add(new Sample("monitor_samples", samples, Metric.Type.GAUGE, labels));
        metrics.add(new Sample("monitor_scrape_interval_seconds", scrapeIntervalMs / 1000d, Metric.Type.GAUGE, labels));
        metrics.add(new Sample("monitor_last_collect_timestamp_seconds",
                snapshot.collectedAtMillis / 1000d, Metric.Type.GAUGE, labels));
        for (Map.Entry<DropCounter.Reason, Long> entry : droppedTotals.entrySet()) {
            Map<String, String> dropLabels = new LinkedHashMap<>(labels);
            dropLabels.put("reason", DropCounter.label(entry.getKey()));
            metrics.add(new Sample("monitor_dropped_samples_total", entry.getValue(), Metric.Type.COUNTER, dropLabels));
        }
        for (Target target : snapshot.targets) {
            Map<String, String> targetLabels = new LinkedHashMap<>(labels);
            targetLabels.put("kind", target.kind);
            targetLabels.put("target", target.target);
            metrics.add(new Sample("monitor_target_up", target.up ? 1 : 0, Metric.Type.GAUGE, targetLabels));
        }
        return metrics;
    }

    // 过滤语义：app/provider 各自选中对应来源（未指定则不限制来源），instance 对全部样本生效
    private static boolean matches(ScrapeQuery query, Sample metric) {
        Map<String, String> labels = metric.labels;
        if (query.instance != null && !query.instance.isEmpty()
                && !query.instance.equals(labels.get("qingzhou_instance"))) return false;
        if (query.app != null && !query.app.isEmpty()) return query.app.equals(labels.get("app"));
        if (query.provider != null && !query.provider.isEmpty()) return query.provider.equals(labels.get("provider"));
        return true;
    }

    private static boolean expired(long deadlineNanos) {
        return System.nanoTime() - deadlineNanos >= 0;
    }

    private static long parseConfig(Map<String, String> config, String key, long defaultValue, long min, long max) {
        String raw = config.get(key);
        if (raw == null || raw.trim().isEmpty()) return defaultValue;
        long value;
        try {
            value = Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number: " + raw);
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException(key + " out of [" + min + "," + max + "]: " + value);
        }
        return value;
    }

    private static final class ScrapeQuery {
        final String app;
        final String instance;
        final String provider;
        final String[] roles;
        final String rolesKey;

        ScrapeQuery(String app, String instance, String provider, String[] roles) {
            this.app = app;
            this.instance = instance;
            this.provider = provider;
            this.roles = roles;
            this.rolesKey = rolesKey(roles);
        }

        // 角色顺序与重复不影响缓存身份，否则不同令牌会互相踢缓存
        private static String rolesKey(String[] roles) {
            if (roles == null || roles.length == 0) return "";
            Set<String> unique = new TreeSet<>();
            for (String role : roles) {
                if (role != null) unique.add(role);
            }
            return String.join(",", unique);
        }
    }

    private static final class Target {
        final String kind;
        final String instance;
        final String target;
        final boolean up;

        Target(String kind, String instance, String target, boolean up) {
            this.kind = kind;
            this.instance = instance;
            this.target = target;
            this.up = up;
        }
    }

    private static final class Cache {
        final List<Sample> metrics;
        final long collectedAtMillis;
        final long durationMs;
        final long registryVersion;
        final long providerVersion;
        final List<Target> targets;

        Cache(List<Sample> metrics, long collectedAtMillis, long durationMs, long registryVersion,
              long providerVersion, List<Target> targets) {
            this.metrics = metrics;
            this.collectedAtMillis = collectedAtMillis;
            this.durationMs = durationMs;
            this.registryVersion = registryVersion;
            this.providerVersion = providerVersion;
            this.targets = targets;
        }
    }
}
