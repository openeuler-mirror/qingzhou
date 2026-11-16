package qingzhou.agent;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.MetricProvider;
import qingzhou.registry.AgentDomainService;
import qingzhou.registry.Constants;

/**
 * 远端实例全量指标端点。免认证不等于无凭证：凭证是每实例密钥与绑定本端点的 AAD
 * （见 AgentDomainService），会话身份不参与此边界。
 * 抓取要遍历本地 provider，故异步派发到业务线程池，不占用网络 IO 线程；
 * 通过 target 排除远程 provider，本端点绝不发起远程访问。
 */
@Component(property = {
        HttpHandler.HANDLE_PATH + "=" + Constants.REMOTE_METRICS_URI,
        HttpHandler.HANDLE_NO_AUTH + "=true",
        HttpHandler.HANDLE_METHODS + "=POST"},
        service = HttpHandler.class)
public class Metrics extends AbstractAgentHandler {
    // 未声明 PROVIDER_LOCAL 即本地；仅排除显式 false 的跨实例出口
    private static final String LOCAL_TARGET = "(!(" + MetricProvider.PROVIDER_LOCAL + "=false))";
    private final Map<MetricProvider, String> providers = new ConcurrentHashMap<>();

    @Reference
    private Json json;

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            target = LOCAL_TARGET)
    protected void bindMetricProvider(MetricProvider provider, Map<String, Object> properties) {
        Object id = properties.get(MetricProvider.PROVIDER_ID);
        providers.put(provider, (String) id);
    }

    protected void unbindMetricProvider(MetricProvider provider) {
        providers.remove(provider);
    }

    @Reference
    public void setAgentDomainKeys(AgentDomainService agentDomainService) {
        this.agentDomainService = agentDomainService;
    }

    @Reference
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Reference
    public void setAgentCipherManager(AgentCipherManager agentCipherManager) {
        this.agentCipherManager = agentCipherManager;
    }

    @Reference
    public void setAgentInstanceManager(AgentInstanceManager agentInstanceManager) {
        this.agentInstanceManager = agentInstanceManager;
    }

    @Override
    protected String uriName() {
        return Constants.REMOTE_METRICS_URI;
    }

    @Override
    protected String doProcess(byte[] requestData, HttpRequest httpRequest) throws Throwable {
        Map<String, Set<Metric>> byProvider = new LinkedHashMap<>();
        providers.forEach((provider, id) ->
                byProvider.computeIfAbsent(id, key -> new LinkedHashSet<>()).addAll(provider.collectMetrics()));
        // 与本类同名的 DTO 须用全限定名
        return json.toJson(qingzhou.monitor.api.Metrics.of(byProvider));
    }
}
