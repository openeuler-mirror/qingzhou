package qingzhou.registry.monitor;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.api.AppContext;
import qingzhou.api.FieldType;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.*;
import qingzhou.logger.Logger;
import qingzhou.monitor.api.Metric;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.Constants;
import qingzhou.registry.Registry;

public class AppMonitorMetricsTest {

    @Test
    public void monitorableModel_collect_emitNumericAndTextSamples() throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put("count", "42");
        data.put("home", "/usr/lib/jvm");
        data.put("name", "ignored");
        AppMonitorMetrics provider = provider(new LocalStub(app(model(true)), data, false));
        Set<Metric> metrics = provider.collectMetrics();
        Assert.assertEquals(find(metrics, "monitor_value", "field", "count").value(), 42d);
        Assert.assertEquals(find(metrics, "monitor_info", "field", "home").labels().get("value"), "/usr/lib/jvm");
        Assert.assertNull(find(metrics, "monitor_value", "field", "name")); // 非监视字段不采集
        Assert.assertNull(find(metrics, "monitor_info", "field", "name"));
        Assert.assertEquals(find(metrics, "monitor_targets").value(), 1d);
    }

    @Test
    public void modelWithoutMonitorAction_collect_skipTarget() throws Exception {
        AppMonitorMetrics provider = provider(new LocalStub(app(model(false)), Collections.emptyMap(), false));
        Set<Metric> metrics = provider.collectMetrics();
        Assert.assertEquals(metrics.size(), 1); // 仅 monitor_targets
        Assert.assertEquals(metrics.iterator().next().value(), 0d);
    }

    @Test
    public void failingInvoke_collect_countErrorAndKeepServing() throws Exception {
        AppMonitorMetrics provider = provider(new LocalStub(app(model(true)), Collections.emptyMap(), true));
        Set<Metric> metrics = provider.collectMetrics();
        Assert.assertEquals(find(metrics, "monitor_errors_total", "stage", "invoke").value(), 1d);
    }

    private static Registry registryStub(AppStubLocal localApp) {
        Map<String, AppStubLocal> localApps = Collections.singletonMap("app1", localApp);
        return (Registry) Proxy.newProxyInstance(AppMonitorMetricsTest.class.getClassLoader(), new Class<?>[]{Registry.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getAllLocalApps":
                            return new ArrayList<>(localApps.keySet());
                        case "getLocalApp":
                            return localApps.get(args[0]);
                        default: {
                            Class<?> type = method.getReturnType();
                            if (type == boolean.class) return false;
                            if (type == int.class) return 0;
                            if (type == long.class) return 0L;
                            return null;
                        }
                    }
                });
    }

    private static AppMonitorMetrics provider(AppStubLocal localApp) throws Exception {
        AppMonitorMetrics provider = new AppMonitorMetrics();
        setField(provider, "registry", registryStub(localApp));
        setField(provider, "logger", loggerStub());
        return provider;
    }

    private static AppMeta app(Model model) {
        App app = new App();
        app.code = "app1";
        app.models.add(model);
        AppMeta appMeta = new AppMeta();
        appMeta.setApp(app);
        return appMeta;
    }

    private static Model model(boolean withMonitorAction) {
        Model model = new Model();
        model.code = "m1";
        if (withMonitorAction) {
            ModelAction action = new ModelAction();
            action.code = Constants.ACTION_CODE_MONITOR;
            action.isDefaultAction = true;
            model.actions.add(action);
        }
        model.fields.add(field("count", FieldType.monitor));
        model.fields.add(field("home", FieldType.monitor));
        model.fields.add(field("name", FieldType.form));
        return model;
    }

    private static ModelField field(String code, FieldType type) {
        ModelField field = new ModelField();
        field.code = code;
        field.field_type = type;
        return field;
    }

    private static Metric find(Set<Metric> metrics, String name) {
        for (Metric metric : metrics) {
            if (metric.name().equals(name)) {
                return metric;
            }
        }
        return null;
    }

    private static Metric find(Set<Metric> metrics, String name, String labelKey, String labelValue) {
        for (Metric metric : metrics) {
            if (metric.name().equals(name) && labelValue.equals(metric.labels().get(labelKey))) {
                return metric;
            }
        }
        return null;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Logger loggerStub() {
        return (Logger) Proxy.newProxyInstance(AppMonitorMetricsTest.class.getClassLoader(), new Class<?>[]{Logger.class},
                (proxy, method, args) -> method.getReturnType() == boolean.class ? false : null);
    }

    private static final class LocalStub implements AppStubLocal {
        private final AppMeta appMeta;
        private final Map<String, String> data;
        private final boolean fail;

        private LocalStub(AppMeta appMeta, Map<String, String> data, boolean fail) {
            this.appMeta = appMeta;
            this.data = data;
            this.fail = fail;
        }

        @Override
        public AppMeta getAppMeta() {
            return appMeta;
        }

        @Override
        public AppContext getAppContext() {
            return null;
        }

        @Override
        public void invokeApp(RequestImpl request) throws Throwable {
            if (fail) throw new IllegalStateException("app unavailable");
            request.getResponse().data(data);
        }
    }
}
