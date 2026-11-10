package qingzhou.monitor.collector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.FieldType;
import qingzhou.api.action.Monitor;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.*;
import qingzhou.monitor.Metric;
import qingzhou.monitor.prometheus.DropCounter;
import qingzhou.monitor.prometheus.Sample;
import qingzhou.registry.AppStub;
import qingzhou.registry.PermissionChecker;

/**
 * 采集单个应用的数值监控字段为端点样本。应用侧 monitor 动作返回弱类型字符串 Map，
 * 数值契约在此边界一次性收敛：非数值字段丢弃并计数，绝不因单个字段把整次抓取打挂。
 */
@Component
public class MonitorCollector {
    @Reference
    private PermissionChecker permissionChecker;

    private final Pattern DECIMAL = Pattern.compile("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?");

    public List<Sample> collect(String instanceId, String appCode, AppStub stub, String[] roles,
                                DropCounter drops) throws Throwable {
        List<Sample> metrics = new ArrayList<>();
        AppMeta appMeta = stub.getAppMeta();
        // 契约适配：注册中心并发上下线时 getAppMeta/getApp 允许为 null
        if (appMeta == null || appMeta.getApp() == null) return metrics;
        App app = appMeta.getApp();

        for (Model model : app.models) {
            List<ModelField> fields = numericMonitorFields(model);
            if (fields.isEmpty() || !hasMonitorAction(model)) continue;
            if (!permissionChecker.isAllowed(app, model.code, Monitor.ACTION_CODE_MONITOR, roles)) continue;

            RequestImpl request = new RequestImpl();
            request.setInstance(instanceId);
            request.setApp(appCode);
            request.setModel(model.code);
            request.setAction(Monitor.ACTION_CODE_MONITOR);
            stub.invokeApp(request);

            ResponseImpl response = request.getResponse();
            if (!response.isSuccess()) {
                throw new IllegalStateException("monitor action failed: model=" + model.code);
            }
            Object data = response.getData();
            if (!(data instanceof Map)) {
                throw new IllegalStateException("monitor action returned no data map: model=" + model.code);
            }
            Map<?, ?> values = (Map<?, ?>) data;
            for (ModelField field : fields) {
                Object raw = values.get(field.code);
                if (raw == null) continue; // 应用未采集到该字段：不输出样本
                String detail = "app=" + appCode + ", model=" + model.code + ", field=" + field.code;
                String text = scalarText(raw);
                if (text == null) {
                    drops.add(DropCounter.Reason.NON_SCALAR, detail);
                    continue;
                }
                Double number = parseFinite(text);
                if (number == null) {
                    drops.add(DropCounter.Reason.NON_NUMERIC, detail);
                    continue;
                }
                Map<String, String> labels = new LinkedHashMap<>();
                labels.put("qingzhou_instance", instanceId);
                labels.put("app", appCode);
                labels.put("model", model.code);
                labels.put("field", field.code);
                metrics.add(new Sample(field.code, number, Metric.Type.GAUGE, labels));
            }
        }
        return metrics;
    }

    private List<ModelField> numericMonitorFields(Model model) {
        List<ModelField> fields = new ArrayList<>();
        for (ModelField field : model.fields) {
            if (field.field_type == FieldType.monitor && field.numeric) fields.add(field);
        }
        return fields;
    }

    private boolean hasMonitorAction(Model model) {
        for (ModelAction action : model.actions) {
            if (Monitor.ACTION_CODE_MONITOR.equals(action.code)) return true;
        }
        return false;
    }

    // 契约适配：远程响应经 JSON 反序列化后 value 可能是 Number/Boolean，而非 String
    private String scalarText(Object raw) {
        if (raw instanceof String) return (String) raw;
        if (raw instanceof Number || raw instanceof Boolean) return String.valueOf(raw);
        return null;
    }

    private Double parseFinite(String text) {
        if (!DECIMAL.matcher(text).matches()) return null;
        double value = Double.parseDouble(text);
        return Double.isFinite(value) ? value : null;
    }
}
