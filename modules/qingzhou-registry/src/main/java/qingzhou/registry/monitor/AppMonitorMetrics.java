package qingzhou.registry.monitor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.FieldType;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.Model;
import qingzhou.dto.meta.ModelField;
import qingzhou.logger.Logger;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.MetricProvider;
import qingzhou.registry.AppStub;
import qingzhou.registry.Constants;
import qingzhou.registry.Registry;

/**
 * 本地应用监视数据（Monitor SPI）的指标出口：随 /metrics 抓取同步拉取，不维护后台轮询。
 * 数值项出 monitor_value 样本；路径、版本等非数值项按 Prometheus info-metric 惯例写进 label。
 */
@Component(property = MetricProvider.PROVIDER_ID + "=app")
public class AppMonitorMetrics implements MetricProvider {
    @Reference
    private Registry registry;
    @Reference
    private Logger logger;

    // 采集失败按阶段计数：抓取是只读观测面，单目标失败不得打断整次抓取，失败以 counter 暴露而非静默
    private final Map<String, AtomicLong> errors = new ConcurrentHashMap<>();

    @Override
    public Set<Metric> collectMetrics() {
        List<Target> targets = targets();
        Set<Metric> metrics = new HashSet<>();
        metrics.add(Metric.of("monitor_targets", targets.size(), Metric.Type.GAUGE,
                "Local application monitoring targets fetched in this scrape"));
        for (Target target : targets) {
            metrics.addAll(collect(target));
        }
        errors.forEach((stage, count) -> metrics.add(Metric.of("monitor_errors_total", count.get(),
                Metric.Type.COUNTER, Collections.singletonMap("stage", stage),
                "Monitoring collections that failed, by stage")));
        return metrics;
    }

    private List<Target> targets() {
        List<Target> targets = new ArrayList<>();
        for (String app : registry.getAllLocalApps()) {
            AppStub stub = registry.getLocalApp(app);
            if (stub == null) continue; // 并发契约：应用可能在枚举期间被卸载
            for (Model model : stub.getAppMeta().getApp().models) {
                // 只有实现了 Monitor 且声明了监视字段的模型才有监视数据
                boolean isMonitorable = model.actions.stream().anyMatch(action -> Constants.ACTION_CODE_MONITOR.equals(action.code))
                        && model.fields.stream().anyMatch(field -> field.field_type == FieldType.monitor);
                if (isMonitorable) {
                    targets.add(new Target(app, model, stub));
                }
            }
        }
        return targets;
    }

    private Set<Metric> collect(Target target) {
        RequestImpl request = new RequestImpl();
        request.setApp(target.app);
        request.setModel(target.model.code);
        request.setAction(Constants.ACTION_CODE_MONITOR);
        try {
            target.stub.invokeApp(request);
        } catch (Throwable e) {
            failed("invoke");
            logger.warn("failed to collect monitoring data of " + target + ": " + e.getMessage());
            return Collections.emptySet();
        }

        ResponseImpl response = request.getResponse();
        if (!response.isSuccess()) {
            failed("response");
            return Collections.emptySet();
        }
        Object data = response.getData();
        if (!(data instanceof Map)) return Collections.emptySet(); // 应用未提供监视数据

        Set<Metric> metrics = new HashSet<>();
        Map<?, ?> values = (Map<?, ?>) data;
        for (ModelField field : target.model.fields) {
            if (field.field_type != FieldType.monitor) continue;
            Object value = values.get(field.code);
            if (value != null) {
                metrics.add(sample(target, field.code, String.valueOf(value)));
            }
        }
        return metrics;
    }

    // Prometheus 只有数值样本：可解析的按 gauge 上报，其余按 info-metric 惯例以 label 承载
    private static Metric sample(Target target, String field, String value) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", target.app);
        labels.put("model", target.model.code);
        labels.put("field", field);
        try {
            double number = Double.parseDouble(value);
            // 非有限值不受 Prometheus 文本格式支持，与字符串项同归 info
            if (Double.isFinite(number)) {
                return Metric.of("monitor_value", number, Metric.Type.GAUGE, labels);
            }
        } catch (NumberFormatException ignored) {
        }
        labels.put("value", value);
        return Metric.of("monitor_info", 1, Metric.Type.GAUGE, labels);
    }

    private void failed(String stage) {
        errors.computeIfAbsent(stage, key -> new AtomicLong()).incrementAndGet();
    }

    private static final class Target {
        private final String app;
        private final Model model;
        private final AppStub stub;

        private Target(String app, Model model, AppStub stub) {
            this.app = app;
            this.model = model;
            this.stub = stub;
        }

        @Override
        public String toString() {
            return app + "/" + model.code;
        }
    }
}
