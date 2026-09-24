package qingzhou.monitor.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.api.AppContext;
import qingzhou.api.FieldType;
import qingzhou.api.action.Monitor;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.http.server.HttpResponse;
import qingzhou.registry.AppStub;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.AppStubRemote;
import qingzhou.registry.Registry;

public class PrometheusEndpointTest {

    @Test
    public void numericValue_handle_losslessOutput() throws Exception {
        String out = invoke(map("heapUsed", "119"), model("jvm", field("heapUsed", "堆内存使用(MB)")));
        Assert.assertTrue(out.contains("qingzhou_heap_used{instance=\"-\",app=\"demo\",model=\"jvm\",field=\"heapUsed\"} 119.0"));
    }

    @Test
    public void booleanValue_handle_mapsToOneZero() throws Exception {
        String out = invoke(map("enabled", "true", "disabled", "false"),
                model("jvm", field("enabled", "启用"), field("disabled", "禁用")));
        Assert.assertTrue(out.contains("qingzhou_enabled{instance=\"-\",app=\"demo\",model=\"jvm\",field=\"enabled\"} 1.0"));
        Assert.assertTrue(out.contains("qingzhou_disabled{instance=\"-\",app=\"demo\",model=\"jvm\",field=\"disabled\"} 0.0"));
    }

    @Test
    public void nonNumericValue_handle_skipped() throws Exception {
        String out = invoke(map("heapUsed", "119", "statsTime", "2026-09-18 14:00:00"),
                model("jvm", field("heapUsed", "堆内存使用(MB)"), field("statsTime", "统计时间")));
        Assert.assertTrue(out.contains("qingzhou_heap_used"));
        Assert.assertFalse(out.contains("stats_time"));
    }

    @Test
    public void camelCaseField_handle_snakeCaseMetricName() throws Exception {
        String out = invoke(map("cpuProcessUsage", "12.5"), model("jvm", field("cpuProcessUsage", "进程CPU使用率(%)")));
        Assert.assertTrue(out.contains("qingzhou_cpu_process_usage{instance=\"-\",app=\"demo\",model=\"jvm\",field=\"cpuProcessUsage\"}"));
    }

    @Test
    public void reservedSuffixField_handle_strippedMetricName() throws Exception {
        String out = invoke(map("threadCount", "42", "slowLogTotal", "5"),
                model("jvm", field("threadCount", "活动线程数"), field("slowLogTotal", "慢日志总数")));
        Assert.assertTrue(out.contains("qingzhou_thread{"));
        Assert.assertTrue(out.contains("qingzhou_slow_log{"));
        Assert.assertFalse(out.contains("qingzhou_thread_count"));
        Assert.assertFalse(out.contains("qingzhou_slow_log_total"));
    }

    @Test
    public void totalPrefixField_handle_preservedMetricName() throws Exception {
        String out = invoke(map("totalKeys", "100"), model("stat", field("totalKeys", "总Key数")));
        Assert.assertTrue(out.contains("qingzhou_total_keys{"));
    }

    @Test
    public void helpWithNewline_handle_escaped() throws Exception {
        String out = invoke(map("heapUsed", "1"), model("jvm", field("heapUsed", "第一行\n第二行")));
        Assert.assertTrue(out.contains("# HELP qingzhou_heap_used 第一行\\n第二行\n"));
    }

    @Test
    public void sharedFieldAcrossModels_handle_singleHelpType() throws Exception {
        String out = invoke(map("used", "10"), model("jvm", field("used", "已用")), model("os", field("used", "已用")));
        Assert.assertEquals(count(out, "# HELP qingzhou_used"), 1);
        Assert.assertEquals(count(out, "# TYPE qingzhou_used"), 1);
        Assert.assertEquals(count(out, "qingzhou_used{"), 2);
    }

    @Test
    public void remoteAppStub_handle_aggregatedViaRegistry() throws Exception {
        PrometheusEndpoint endpoint = new PrometheusEndpoint();
        AppStubRemote stub = remoteStub("remote-app", map("heapUsed", "63"), model("jvm", field("heapUsed", "堆内存使用(MB)")));
        setField(endpoint, "registry", remoteRegistry("inst-1", "remote-app", stub));
        StubHttpResponse response = new StubHttpResponse();
        endpoint.handle(null, response);

        Assert.assertTrue(response.body.contains("# HELP qingzhou_heap_used 堆内存使用(MB)"));
        Assert.assertTrue(response.body.contains("qingzhou_heap_used{instance=\"inst-1\",app=\"remote-app\",model=\"jvm\",field=\"heapUsed\"} 63.0"));
    }

    @Test
    public void remoteInstance_handle_deduplicatedHelpType() throws Exception {
        PrometheusEndpoint endpoint = new PrometheusEndpoint();
        AppStubLocal localStub = (AppStubLocal) localStub("demo", map("heapUsed", "119"), model("jvm", field("heapUsed", "堆内存使用(MB)")));
        AppStubRemote remoteStub = remoteStub("demo", map("heapUsed", "63"), model("jvm", field("heapUsed", "堆内存使用(MB)")));

        setField(endpoint, "registry", new Registry() {
            public long getRegistryDataVersion() {
                return 0;
            }

            public AppStub getAppStub(String instanceId, String appCode) {
                return null;
            }

            public InstanceInfo getLocalInstance() {
                return null;
            }

            public List<String> getAllLocalApps() {
                return Collections.singletonList("demo");
            }

            public AppStubLocal getLocalApp(String appCode) {
                return localStub;
            }

            public List<String> getAllRemoteInstances() {
                return Collections.singletonList("inst-1");
            }

            public InstanceInfo getRemoteInstance(String instanceId) {
                return null;
            }

            public List<String> getAllRemoteApps(String instanceId) {
                return Collections.singletonList("demo");
            }

            public AppStubRemote getRemoteApp(String instanceId, String appCode) {
                return remoteStub;
            }
        });

        StubHttpResponse response = new StubHttpResponse();
        endpoint.handle(null, response);

        Assert.assertEquals(count(response.body, "# HELP qingzhou_heap_used"), 1);
        Assert.assertEquals(count(response.body, "# TYPE qingzhou_heap_used"), 1);
        Assert.assertEquals(count(response.body, "qingzhou_heap_used{"), 2);
    }

    @Test
    public void remoteException_handle_faultTolerantAndStatus200() throws Exception {
        PrometheusEndpoint endpoint = new PrometheusEndpoint();
        AppStubLocal localStub = (AppStubLocal) localStub("local-app", map("heapUsed", "119"), model("jvm", field("heapUsed", "堆内存使用(MB)")));
        AppStubRemote faultyStub = new AppStubRemote() {
            public AppMeta getAppMeta() {
                return localStub.getAppMeta();
            }

            public void invokeApp(RequestImpl request) throws Throwable {
                throw new java.net.SocketTimeoutException("connection timeout");
            }
        };

        setField(endpoint, "registry", new Registry() {
            public long getRegistryDataVersion() {
                return 0;
            }

            public AppStub getAppStub(String instanceId, String appCode) {
                return null;
            }

            public InstanceInfo getLocalInstance() {
                return null;
            }

            public List<String> getAllLocalApps() {
                return Collections.singletonList("local-app");
            }

            public AppStubLocal getLocalApp(String appCode) {
                return localStub;
            }

            public List<String> getAllRemoteInstances() {
                return Collections.singletonList("faulty-inst");
            }

            public InstanceInfo getRemoteInstance(String instanceId) {
                return null;
            }

            public List<String> getAllRemoteApps(String instanceId) {
                return Collections.singletonList("faulty-app");
            }

            public AppStubRemote getRemoteApp(String instanceId, String appCode) {
                return faultyStub;
            }
        });

        StubHttpResponse response = new StubHttpResponse();
        endpoint.handle(null, response);

        Assert.assertNotNull(response.body);
        Assert.assertTrue(response.body.contains("qingzhou_heap_used{instance=\"-\",app=\"local-app\",model=\"jvm\",field=\"heapUsed\"} 119.0"));
        Assert.assertFalse(response.body.contains("faulty-inst"));
    }

    @Test
    public void remoteSecurityException_handle_faultTolerantAndStatus200() throws Exception {
        PrometheusEndpoint endpoint = new PrometheusEndpoint();
        AppStubRemote cryptoErrorStub = new AppStubRemote() {
            public AppMeta getAppMeta() {
                return buildAppMeta("demo", model("jvm", field("heapUsed", "内存")));
            }

            public void invokeApp(RequestImpl request) throws Throwable {
                throw new SecurityException("key auth error");
            }
        };
        setField(endpoint, "registry", remoteRegistry("sec-inst", "demo", cryptoErrorStub));
        StubHttpResponse response = new StubHttpResponse();
        endpoint.handle(null, response);
        Assert.assertNotNull(response.body);
        Assert.assertFalse(response.body.contains("sec-inst"));
    }

    @Test
    public void nullAppMeta_handle_gracefullySkipped() throws Exception {
        PrometheusEndpoint endpoint = new PrometheusEndpoint();
        AppStubLocal nullMetaStub = new AppStubLocal() {
            public AppMeta getAppMeta() {
                return null;
            }

            public AppContext getAppContext() {
                return null;
            }

            public void invokeApp(RequestImpl request) {
            }
        };
        Map<String, AppStubLocal> apps = new HashMap<>();
        apps.put("null-app", nullMetaStub);
        setField(endpoint, "registry", new Registry() {
            public long getRegistryDataVersion() { return 0; }
            public AppStub getAppStub(String instanceId, String appCode) { return null; }
            public InstanceInfo getLocalInstance() { return null; }
            public List<String> getAllLocalApps() { return Collections.singletonList("null-app"); }
            public AppStubLocal getLocalApp(String appCode) { return nullMetaStub; }
            public List<String> getAllRemoteInstances() { return Collections.emptyList(); }
            public InstanceInfo getRemoteInstance(String instanceId) { return null; }
            public List<String> getAllRemoteApps(String instanceId) { return null; }
            public AppStubRemote getRemoteApp(String instanceId, String appCode) { return null; }
        });
        StubHttpResponse response = new StubHttpResponse();
        endpoint.handle(null, response);
        Assert.assertEquals(response.body, "");
    }

    @Test
    public void nullRemoteApps_handle_gracefullyHandled() throws Exception {
        PrometheusEndpoint endpoint = new PrometheusEndpoint();
        setField(endpoint, "registry", new Registry() {
            public long getRegistryDataVersion() { return 0; }
            public AppStub getAppStub(String instanceId, String appCode) { return null; }
            public InstanceInfo getLocalInstance() { return null; }
            public List<String> getAllLocalApps() { return Collections.emptyList(); }
            public AppStubLocal getLocalApp(String appCode) { return null; }
            public List<String> getAllRemoteInstances() { return Collections.singletonList("inst-empty"); }
            public InstanceInfo getRemoteInstance(String instanceId) { return null; }
            public List<String> getAllRemoteApps(String instanceId) { return null; }
            public AppStubRemote getRemoteApp(String instanceId, String appCode) { return null; }
        });
        StubHttpResponse response = new StubHttpResponse();
        endpoint.handle(null, response);
        Assert.assertEquals(response.body, "");
    }

    @Test
    public void multipleRemoteInstances_handle_isolatedLabels() throws Exception {
        PrometheusEndpoint endpoint = new PrometheusEndpoint();
        AppStubRemote stub1 = remoteStub("shared-app", map("cpu", "25"), model("os", field("cpu", "CPU使用率")));
        AppStubRemote stub2 = remoteStub("shared-app", map("cpu", "75"), model("os", field("cpu", "CPU使用率")));

        setField(endpoint, "registry", new Registry() {
            public long getRegistryDataVersion() { return 0; }
            public AppStub getAppStub(String instanceId, String appCode) { return null; }
            public InstanceInfo getLocalInstance() { return null; }
            public List<String> getAllLocalApps() { return Collections.emptyList(); }
            public AppStubLocal getLocalApp(String appCode) { return null; }
            public List<String> getAllRemoteInstances() {
                List<String> list = new ArrayList<>();
                list.add("node-1");
                list.add("node-2");
                return list;
            }
            public InstanceInfo getRemoteInstance(String instanceId) { return null; }
            public List<String> getAllRemoteApps(String instanceId) { return Collections.singletonList("shared-app"); }
            public AppStubRemote getRemoteApp(String instanceId, String appCode) {
                return "node-1".equals(instanceId) ? stub1 : stub2;
            }
        });

        StubHttpResponse response = new StubHttpResponse();
        endpoint.handle(null, response);

        Assert.assertEquals(count(response.body, "# HELP qingzhou_cpu"), 1);
        Assert.assertEquals(count(response.body, "# TYPE qingzhou_cpu"), 1);
        Assert.assertTrue(response.body.contains("qingzhou_cpu{instance=\"node-1\",app=\"shared-app\",model=\"os\",field=\"cpu\"} 25.0"));
        Assert.assertTrue(response.body.contains("qingzhou_cpu{instance=\"node-2\",app=\"shared-app\",model=\"os\",field=\"cpu\"} 75.0"));
    }

    @Test
    public void negativeAndZeroValues_handle_losslessOutput() throws Exception {
        String out = invoke(map("temp", "-15.5", "idle", "0"), model("sensor", field("temp", "温度"), field("idle", "空闲")));
        Assert.assertTrue(out.contains("qingzhou_temp{instance=\"-\",app=\"demo\",model=\"sensor\",field=\"temp\"} -15.5"));
        Assert.assertTrue(out.contains("qingzhou_idle{instance=\"-\",app=\"demo\",model=\"sensor\",field=\"idle\"} 0.0"));
    }

    @Test
    public void helpWithBackslash_handle_escaped() throws Exception {
        String out = invoke(map("path", "1"), model("fs", field("path", "C:\\qingzhou\\path")));
        Assert.assertTrue(out.contains("# HELP qingzhou_path C:\\\\qingzhou\\\\path\n"));
    }

    private String invoke(Map<String, String> monitorData, Model... models) throws Exception {
        PrometheusEndpoint endpoint = new PrometheusEndpoint();
        setField(endpoint, "registry", localRegistry(monitorData, models));
        StubHttpResponse response = new StubHttpResponse();
        endpoint.handle(null, response);
        return response.body;
    }

    private Registry localRegistry(final Map<String, String> monitorData, final Model... models) {
        final AppStubLocal stub = (AppStubLocal) localStub("demo", monitorData, models);
        final Map<String, AppStubLocal> apps = new HashMap<>();
        apps.put("demo", stub);
        return new Registry() {
            public long getRegistryDataVersion() {
                return 0;
            }

            public AppStub getAppStub(String instanceId, String appCode) {
                return null;
            }

            public InstanceInfo getLocalInstance() {
                return null;
            }

            public List<String> getAllLocalApps() {
                return new ArrayList<>(apps.keySet());
            }

            public AppStubLocal getLocalApp(String appCode) {
                return apps.get(appCode);
            }

            public List<String> getAllRemoteInstances() {
                return new ArrayList<>();
            }

            public InstanceInfo getRemoteInstance(String instanceId) {
                return null;
            }

            public List<String> getAllRemoteApps(String instanceId) {
                return null;
            }

            public AppStubRemote getRemoteApp(String instanceId, String appCode) {
                return null;
            }
        };
    }

    private Registry remoteRegistry(final String instanceId, final String appCode, final AppStubRemote stub) {
        return new Registry() {
            public long getRegistryDataVersion() {
                return 0;
            }

            public AppStub getAppStub(String id, String app) {
                return null;
            }

            public InstanceInfo getLocalInstance() {
                return null;
            }

            public List<String> getAllLocalApps() {
                return new ArrayList<>();
            }

            public AppStubLocal getLocalApp(String app) {
                return null;
            }

            public List<String> getAllRemoteInstances() {
                return Collections.singletonList(instanceId);
            }

            public InstanceInfo getRemoteInstance(String id) {
                return null;
            }

            public List<String> getAllRemoteApps(String id) {
                return Collections.singletonList(appCode);
            }

            public AppStubRemote getRemoteApp(String id, String app) {
                return stub;
            }
        };
    }

    private AppStubLocal localStub(String appCode, Map<String, String> monitorData, Model... models) {
        final AppMeta appMeta = buildAppMeta(appCode, models);
        return new AppStubLocal() {
            public AppMeta getAppMeta() {
                return appMeta;
            }

            public AppContext getAppContext() {
                return null;
            }

            public void invokeApp(RequestImpl request) {
                request.getResponse().data(monitorData);
            }
        };
    }

    private AppStubRemote remoteStub(String appCode, Map<String, String> monitorData, Model... models) {
        final AppMeta appMeta = buildAppMeta(appCode, models);
        return new AppStubRemote() {
            public AppMeta getAppMeta() {
                return appMeta;
            }

            public void invokeApp(RequestImpl request) {
                request.getResponse().data(monitorData);
            }
        };
    }

    private AppMeta buildAppMeta(String appCode, Model... models) {
        AppMeta appMeta = new AppMeta();
        App app = new App();
        app.code = appCode;
        for (Model model : models) app.models.add(model);
        appMeta.setApp(app);
        return appMeta;
    }

    private Model model(String code, ModelField... fields) {
        Model model = new Model();
        model.code = code;
        ModelAction action = new ModelAction();
        action.code = Monitor.ACTION_CODE_MONITOR;
        model.actions.add(action);
        for (ModelField field : fields) model.fields.add(field);
        return model;
    }

    private ModelField field(String code, String name) {
        ModelField field = new ModelField();
        field.code = code;
        field.name = new String[]{name};
        field.field_type = FieldType.monitor;
        return field;
    }

    private Map<String, String> map(String... kvs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) map.put(kvs[i], kvs[i + 1]);
        return map;
    }

    private int count(String text, String sub) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(sub, index)) != -1) {
            count++;
            index += sub.length();
        }
        return count;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class StubHttpResponse implements HttpResponse {
        String body;
        String contentType;

        public void status400Finish() {
        }

        public void status500Finish(String msg) {
        }

        public void redirect(String url) {
        }

        public HttpResponse status(int status) {
            return this;
        }

        public HttpResponse header(String name, String value) {
            return this;
        }

        public HttpResponse contentType(String value) {
            this.contentType = value;
            return this;
        }

        public HttpResponse contentTypeJsonUtf8() {
            return this;
        }

        public HttpResponse send(String bodyAsUtf8) {
            return this;
        }

        public HttpResponse send(byte[] body) {
            return this;
        }

        public void finish() {
        }

        public void sendFinish(String bodyAsUtf8) {
            this.body = bodyAsUtf8;
        }

        public void sendFinish(byte[] body) {
        }
    }
}
