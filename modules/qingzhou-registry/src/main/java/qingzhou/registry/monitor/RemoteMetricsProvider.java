package qingzhou.registry.monitor;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.MetricProvider;
import qingzhou.monitor.api.Metrics;
import qingzhou.registry.AgentDomainService;
import qingzhou.registry.Constants;
import qingzhou.registry.Registry;

/**
 * 跨实例指标出口：每个远端实例一个并发任务，一次抓取汇总全部样本。
 * 声明非 Local，agent 快照按属性过滤掉本出口；本实例未被注册任何远端实例时也直接返回空集。
 */
@Component(property = {
        MetricProvider.PROVIDER_ID + "=remote",
        MetricProvider.PROVIDER_LOCAL + "=false"})
public class RemoteMetricsProvider implements MetricProvider {
    // 实例数上限见 Register.MAX_REMOTE_INSTANCES；固定池避免按实例数无上限建线程
    private static final int FETCH_THREADS = 5;
    // 并发抓取时单个不可达实例不得拖住整次汇总
    private static final int FETCH_TIMEOUT_MILLIS = 3000;

    @Reference
    private Registry registry;
    @Reference
    private AgentDomainService agentDomainService;
    @Reference
    private Json json;
    @Reference
    private HttpClient httpClient;
    @Reference
    private Logger logger;

    private final AtomicLong failures = new AtomicLong();
    private ExecutorService workers;

    @Activate
    void activate() {
        workers = Executors.newFixedThreadPool(FETCH_THREADS, runnable -> {
            Thread thread = new Thread(runnable, "qz-remote-metrics");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Deactivate
    void deactivate() {
        workers.shutdown();
    }

    @Override
    public Set<Metric> collectMetrics() {
        List<String> instances = registry.getAllRemoteInstances();
        if (instances.isEmpty()) {
            return Collections.emptySet();
        }

        List<CompletableFuture<Set<Metric>>> tasks = new ArrayList<>(instances.size());
        for (String instanceId : instances) {
            tasks.add(CompletableFuture.supplyAsync(() -> collect(instanceId), workers)
                    .exceptionally(e -> {
                        // 抓取是只读观测面：单实例失败不得打断整次抓取，以 counter 暴露而非静默跳过
                        failures.incrementAndGet();
                        logger.warn("remote metrics collect failed: " + e.getMessage());
                        return Collections.emptySet();
                    }));
        }
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture<?>[0])).join();

        Set<Metric> metrics = new HashSet<>();
        for (CompletableFuture<Set<Metric>> task : tasks) {
            metrics.addAll(task.join());
        }
        if (failures.get() > 0) {
            metrics.add(Metric.of("remote_metrics_errors_total", failures.get(), Metric.Type.COUNTER,
                    "Remote instance metric collections that failed"));
        }
        return metrics;
    }

    private Set<Metric> collect(String instanceId) {
        Map<String, Set<Metric>> byProvider = fetch(instanceId).getMetricsByProvider();
        Set<Metric> metrics = new HashSet<>();
        for (Map.Entry<String, Set<Metric>> entry : byProvider.entrySet()) {
            for (Metric source : entry.getValue()) {
                Map<String, String> labels = new LinkedHashMap<>(source.getLabels());
                labels.put("instance", instanceId);
                // provider 维度不带在 Metric 里，并入 name 才能在渲染后区分来源
                // 示例：qingzhou_remote_jvm_memory_used{provider="remote",instance="<instanceId>",...}
                metrics.add(Metric.of(entry.getKey() + "_" + source.getName(),
                        source.getValue(), source.getType(), labels, source.getDescription()));
            }
        }
        return metrics;
    }

    private Metrics fetch(String instanceId) {
        try {
            InstanceInfo instance = registry.getRemoteInstance(instanceId);
            if (instance == null) { // 并发契约：健康检查可能在枚举期间移除实例
                throw new IllegalArgumentException("unknown instance");
            }

            String protocol = instance.isSslEnabled() ? "https" : "http";
            String url = protocol + "://" + instance.getHost() + ":" + instance.getPort()
                    + instance.getContextPath() + "/agent" + Constants.REMOTE_METRICS_URI;

            EpochKeyRotatingCipher cipher = agentDomainService.getInstanceCipher(
                    instance.getDomain(), instance.getInstanceSecret());
            byte[] aad = agentDomainService.aadForAgent(instance.getId(), Constants.REMOTE_METRICS_URI);
            // 端点要求非空请求体：全量快照无输入参数，发固定占位
            byte[] encrypted = cipher.encrypt("{}".getBytes(StandardCharsets.UTF_8), aad);
            Response response = httpClient.send(httpClient.buildRequest(url)
                    .trustAllCertificates() // 受控内网降级：跳过注册中心自签证书校验，需 allow_ssl_trust_all=true
                    .connectTimeout(FETCH_TIMEOUT_MILLIS)
                    .readTimeout(FETCH_TIMEOUT_MILLIS)
                    .method(HttpMethod.POST)
                    .body(encrypted));
            if (response.getStatus() != 200) {
                throw new IllegalStateException("remote metrics failed [" + response.getStatus() + "]");
            }

            byte[] decrypted = cipher.decrypt(response.getBody(), agentDomainService.responseAad(aad));
            return json.fromJson(new String(decrypted, StandardCharsets.UTF_8), Metrics.class);
        } catch (Exception e) {
            throw new IllegalStateException("remote fetch failed: " + instanceId, e);
        }
    }
}
