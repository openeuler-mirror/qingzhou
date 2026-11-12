package qingzhou.prometheus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.monitor.Metric;
import qingzhou.monitor.MetricProvider;

/** 仅采集进程内 MetricProvider，无远程 RPC，故同步渲染，由框架统一收尾并兜底 500。 */
@Component(property = {HttpHandler.HANDLE_PATH + "=/prometheus"})
public class PrometheusEndpoint implements HttpHandler {
    private static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    private static final String PROVIDER = "provider";

    private final Map<MetricProvider, String> providers = new ConcurrentHashMap<>();

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void bindMetricProvider(MetricProvider provider, Map<String, Object> properties) {
        // 契约适配：OSGi 组件属性值为 Object，须显式校验类型
        Object id = properties.get(MetricProvider.PROVIDER_ID);
        if (!(id instanceof String)) throw new IllegalArgumentException(MetricProvider.PROVIDER_ID + " must be a String");
        providers.put(provider, (String) id);
    }

    protected void unbindMetricProvider(MetricProvider provider) {
        providers.remove(provider);
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        String filter = request.getParameter(PROVIDER);
        List<Sample> samples = new ArrayList<>();
        for (Map.Entry<MetricProvider, String> entry : providers.entrySet()) {
            String id = entry.getValue();
            if (filter != null && !filter.isEmpty() && !filter.equals(id)) continue;
            Map<String, String> labels = Collections.singletonMap(PROVIDER, id);
            for (Metric metric : entry.getKey().collectMetrics()) {
                samples.add(Sample.of(metric, id + "_" + metric.name(), labels));
            }
        }
        response.contentType(CONTENT_TYPE).send(MetricFormat.render(samples));
    }
}
