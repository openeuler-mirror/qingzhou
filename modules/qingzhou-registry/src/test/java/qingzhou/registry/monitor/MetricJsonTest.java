package qingzhou.registry.monitor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.json.Json;
import qingzhou.json.impl.JsonImpl;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.Metrics;

public class MetricJsonTest {

    private static Json json() {
        JsonImpl impl = new JsonImpl();
        impl.init();
        return impl;
    }

    @Test
    public void metrics_jsonRoundTrip_keepAllSamples() throws Exception {
        Map<String, Set<Metric>> byProvider = new LinkedHashMap<>();
        byProvider.put("jvm", Collections.singleton(
                Metric.of("heap_used", 1.5, Metric.Type.GAUGE, "heap")));
        byProvider.put("app", Collections.singleton(
                Metric.of("monitor_value", 0d, Metric.Type.COUNTER,
                        Collections.singletonMap("field", "count"), "")));

        Metrics metrics = Metrics.of(byProvider);
        Metrics restored = json().fromJson(json().toJson(metrics), Metrics.class);

        Assert.assertEquals(restored.getMetricsByProvider().keySet(), byProvider.keySet());
        Metric heap = restored.getMetricsByProvider().get("jvm").iterator().next();
        Assert.assertEquals(heap.getName(), "heap_used");
        Assert.assertEquals(heap.getValue(), 1.5d);
        Assert.assertEquals(heap.getType(), Metric.Type.GAUGE);
        Assert.assertEquals(heap.getDescription(), "heap");
        Assert.assertTrue(heap.getLabels().isEmpty());
        Assert.assertEquals(restored.getMetricsByProvider().get("app").iterator().next()
                .getLabels().get("field"), "count");
    }

    @Test
    public void emptyMetrics_jsonRoundTrip_keepEmptyGroups() throws Exception {
        Metrics restored = json().fromJson(
                json().toJson(Metrics.of(new LinkedHashMap<>())), Metrics.class);
        Assert.assertTrue(restored.getMetricsByProvider().isEmpty());
    }
}
