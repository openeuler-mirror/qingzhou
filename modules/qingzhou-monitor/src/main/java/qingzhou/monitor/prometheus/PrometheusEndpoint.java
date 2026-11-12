package qingzhou.monitor.prometheus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.*;
import qingzhou.http.server.*;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.MetricProvider;

/**
 * 仅采集进程内 MetricProvider，无远程 RPC，故同步渲染，由框架统一收尾并兜底 500。
 */
@Component(property = {HttpHandler.HANDLE_PATH + "=/prometheus"})
public class PrometheusEndpoint implements HttpHandler {
    private static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    private final Map<MetricProvider, String> providers = new ConcurrentHashMap<>();

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void bindMetricProvider(MetricProvider provider, Map<String, Object> properties) {
        // 契约适配：OSGi 组件属性值为 Object，须显式校验类型
        Object id = properties.get(MetricProvider.PROVIDER_ID);
        if (!(id instanceof String))
            throw new IllegalArgumentException(MetricProvider.PROVIDER_ID + " must be a String");
        providers.put(provider, (String) id);
    }

    protected void unbindMetricProvider(MetricProvider provider) {
        providers.remove(provider);
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        String filter = request.getParameter(MetricFormat.PROVIDER);
        Map<String, Collection<Metric>> metrics = new LinkedHashMap<>();
        for (Map.Entry<MetricProvider, String> entry : providers.entrySet()) {
            String id = entry.getValue();
            if (filter != null && !filter.isEmpty() && !filter.equals(id)) continue;
            metrics.computeIfAbsent(id, key -> new ArrayList<>()).addAll(entry.getKey().collectMetrics());
        }
        response.contentType(CONTENT_TYPE).send(MetricFormat.render(metrics));
    }
}
