package qingzhou.agent;

import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.component.annotations.*;
import qingzhou.common.Configs;
import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.MetricProvider;
import qingzhou.registry.AgentDomainService;
import qingzhou.registry.Constants;

@Component(configurationPid = "qingzhou-agent", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = MetricProvider.PROVIDER_ID + "=agent")
public class Heartbeat implements MetricProvider {
    // 心跳周期默认 6s：单次请求上限 2s，故障探测 + 接管注册合计 ≤10s；沿用客户端默认 60s 会把切换拖到分钟级
    private static final int REQUEST_TIMEOUT_MILLIS = 2000;

    @Reference
    private HttpClient httpClient;
    @Reference
    private Logger logger;
    @Reference
    private Json json;
    @Reference
    private AgentInstanceManager agentInstanceManager;
    @Reference
    private AgentCipherManager agentCipherManager;
    @Reference
    private AgentDomainService agentDomainService;

    // 配置顺序即主备优先级：每轮心跳只向首个可用中心注册，主恢复后自动回切
    private List<String> registerUrls;
    // 上次登记成功的中心；各中心独立分配 instanceId，切换后必须重新完整注册。仅心跳线程读写
    private String activeUrl;

    private Timer timer;
    // 是否已被 activeUrl 登记（当前 instanceId + secret 已上报且中心返回 200/304）；由监控抓取线程读取，须 volatile
    private volatile boolean registered;

    // 心跳结果与失败原因分列：区分"中心拒绝"与"网络不可达"是定位注册问题的关键
    private final AtomicLong renewed = new AtomicLong();
    private final AtomicLong registeredCount = new AtomicLong();
    private final AtomicLong errorResponses = new AtomicLong();
    private final AtomicLong connectFailures = new AtomicLong();
    private final AtomicLong networkFailures = new AtomicLong();

    @Activate
    public void start(Map<String, String> config) {
        Configs configs = new Configs(config);
        List<String> urls = configs.splitList("url");
        if (urls.isEmpty()) { // 配置是外部输入边界：至少一个中心，缺少即激活失败
            throw new IllegalArgumentException("url must be configured");
        }
        registerUrls = new ArrayList<>(urls.size());
        for (String url : urls) {
            String baseUrl = url;
            while (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            registerUrls.add(baseUrl + "/registry" + Constants.REMOTE_REGISTER_URI);
        }

        // 固定速率：故障探测耗时不得推迟下一周期，否则主恢复回切与接管窗口会随失败请求累积漂移
        timer = new Timer("agent-heartbeat");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (agentInstanceManager.instanceChanged()) {
                        agentInstanceManager.reCacheInstanceInfo();
                        registered = false; // 新 id + 新 secret，需重新完整注册
                    }

                    send(); // 心跳即注册：注册报文同时承担存活保活（registry 健康检查依据 lastRefreshTime）。
                } catch (Exception e) {
                    logger.error("heartbeat service error", e);
                }
            }
        }, 0L, 1000L * configs.atLeast("interval", 2, 6));
    }

    @Deactivate
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    /**
     * 注册状态是实例是否被中心纳管的一手信号：registered 为 0 且心跳持续失败即通道中断。
     */
    @Override
    public Set<Metric> collectMetrics() {
        Set<Metric> metrics = new HashSet<>();
        metrics.add(Metric.of("registered", registered ? 1 : 0, Metric.Type.GAUGE,
                "Whether this instance is currently registered with the registry"));
        metrics.add(Metric.of("heartbeat_total", renewed.get(), Metric.Type.COUNTER,
                Collections.singletonMap("result", "renew"), "Heartbeats sent, by result"));
        metrics.add(Metric.of("heartbeat_total", registeredCount.get(), Metric.Type.COUNTER,
                Collections.singletonMap("result", "registered"), "Heartbeats sent, by result"));
        metrics.add(Metric.of("heartbeat_total", errorResponses.get(), Metric.Type.COUNTER,
                Collections.singletonMap("result", "error"), "Heartbeats sent, by result"));
        metrics.add(Metric.of("heartbeat_failures_total", connectFailures.get(), Metric.Type.COUNTER,
                Collections.singletonMap("reason", "connect"), "Heartbeats that failed, by reason"));
        metrics.add(Metric.of("heartbeat_failures_total", networkFailures.get(), Metric.Type.COUNTER,
                Collections.singletonMap("reason", "exception"), "Heartbeats that failed, by reason"));
        return metrics;
    }

    // 仅 200/304 视为可用，其余按优先级转向下一个；全部失败即终止本轮并重置 registered。
    // 切换中心不能续期：instanceId 由各中心独立分配，回连 AAD 只认当前 id
    // 回切探测只能用完整注册报文（含 instanceSecret）：各中心同属一个信任域是自动回切的前提
    private void send() {
        boolean canRenew = registered;
        for (String url : registerUrls) {
            if (sendTo(url, canRenew && url.equals(activeUrl))) {
                activeUrl = url;
                registered = true;
                return;
            }
        }
        registered = false;
    }

    private boolean sendTo(String url, boolean canRenew) {
        try {
            Payload payload = getPayload(canRenew);
            String requestUrl = url + "?"
                    + Constants.REMOTE_PARAMETER_DOMAIN_ID + "=" + encode(agentCipherManager.getAgentDomain())
                    + (payload.parameter.isEmpty() ? "" : "&" + payload.parameter);
            byte[] encrypted = payload.cipher.encrypt(payload.data.getBytes(StandardCharsets.UTF_8), payload.aad);
            Request request = httpClient.buildRequest(requestUrl)
                    .trustAllCertificates() // 受控内网降级：跳过注册中心自签证书校验，需 allow_ssl_trust_all=true
                    .connectTimeout(REQUEST_TIMEOUT_MILLIS)
                    .readTimeout(REQUEST_TIMEOUT_MILLIS)
                    .method(HttpMethod.POST)
                    .body(encrypted);
            Response response = httpClient.send(request);

            if (response.getStatus() == 304) {
                renewed.incrementAndGet();
                return true;
            }

            // 200 只对应新实例登记（续期成功恒为 304）：最小心跳在实例已失效时会被解密拒绝回 400，
            // 转向备用中心并以完整注册报文重试
            if (response.getStatus() != 200) {
                errorResponses.incrementAndGet();
                logger.error("response code error [ " + response.getStatus() + " ] : " + url);
                return false;
            }

            InstanceInfo cachedInstance = agentInstanceManager.getCachedInstance();
            byte[] responseAad = agentDomainService.responseAad(payload.aad);
            byte[] registeredInstanceId = payload.cipher.decrypt(response.getBody(), responseAad);
            cachedInstance.setId(new String(registeredInstanceId, StandardCharsets.UTF_8));
            registeredCount.incrementAndGet();
            logger.info(cachedInstance.getAppMetas().size() + " apps registered");
            return true;
        } catch (ConnectException e) {
            connectFailures.incrementAndGet();
            logger.error("failed to connect to the URL: " + url + ", msg: " + e.getMessage());
            return false;
        } catch (Exception e) {
            networkFailures.incrementAndGet();
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8"); // Java 8 无 Charset 重载
    }

    private Payload getPayload(boolean canRenew) throws Exception {
        InstanceInfo cachedInstance = agentInstanceManager.getCachedInstance();
        if (canRenew) { // registrationAad: 有 instanceId 时，用于心跳双向报文的 AAD 计算，无则用于首次注册报文的 AAD 计算
            // 已登记后：续期报文不再序列化 InstanceInfo，且用每实例密钥加密
            // ——既缩小 InstanceInfo.instanceSecret 暴露窗口，又防止同域其他 agent 冒名续期
            return new Payload(agentCipherManager.getInstanceCipher(),
                    json.toJson(Collections.singletonMap("id", cachedInstance.getId())),
                    agentDomainService.aadForRegistry(agentCipherManager.getAgentDomain(), cachedInstance.getId()),
                    Constants.REMOTE_PARAMETER_INSTANCE_ID + "=" + encode(cachedInstance.getId()));
        } else {
            return new Payload(agentCipherManager.getAgentCipher(),
                    json.toJson(cachedInstance),
                    agentDomainService.aadForRegistry(agentCipherManager.getAgentDomain(), null),
                    "");
        }
    }

    private static class Payload {
        final EpochKeyRotatingCipher cipher;
        final String data;
        final byte[] aad;
        final String parameter;

        Payload(EpochKeyRotatingCipher cipher, String data, byte[] aad, String parameter) {
            this.cipher = cipher;
            this.data = data;
            this.aad = aad;
            this.parameter = parameter;
        }
    }
}
