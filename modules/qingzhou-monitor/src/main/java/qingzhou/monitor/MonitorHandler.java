package qingzhou.monitor;

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
import qingzhou.http.server.HttpResponse;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.MetricProvider;
import qingzhou.monitor.api.Metrics;

/**
 * 无 target 绑定全部 MetricProvider（含 remote 出口）后同步渲染，由框架统一收尾并兜底 500。
 */
@Component(property = {HttpHandler.HANDLE_PATH + "=/metrics"})
public class MonitorHandler implements HttpHandler {
    private final Map<MetricProvider, String> providers = new ConcurrentHashMap<>();

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void bindMetricProvider(MetricProvider provider, Map<String, Object> properties) {
        Object id = properties.get(MetricProvider.PROVIDER_ID);
        providers.put(provider, (String) id);
    }

    protected void unbindMetricProvider(MetricProvider provider) {
        providers.remove(provider);
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        Map<String, Set<Metric>> byProvider = new LinkedHashMap<>();
        providers.forEach((provider, id) ->
                byProvider.computeIfAbsent(id, key -> new LinkedHashSet<>()).addAll(provider.collectMetrics()));
        response.contentType(PrometheusFormat.CONTENT_TYPE).send(PrometheusFormat.render(Metrics.of(byProvider)));
    }
}
