package qingzhou.monitor;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;
import qingzhou.http.client.ResponseListener;
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
    public void remoteInstance_handle_aggregatedWithTimeout() throws Exception {
        PrometheusEndpoint endpoint = new PrometheusEndpoint();
        StubRequest request = new StubRequest();
        setField(endpoint, "registry", remoteRegistry("inst-1"));
        setField(endpoint, "httpClient", stubHttpClient(request, remoteText()));
        StubHttpResponse response = new StubHttpResponse();
        endpoint.handle(null, response);

        Assert.assertTrue(response.body.contains("# HELP qingzhou_heap_used 堆内存使用(MB)"));
        Assert.assertTrue(response.body.contains("qingzhou_heap_used{instance=\"inst-1\",app=\"demo\",model=\"jvm\",field=\"heapUsed\"} 63.0"));
        Assert.assertEquals(request.url, "http://host-1:7900/agent/monitor");
        Assert.assertEquals(request.connectTimeout, 3000);
        Assert.assertEquals(request.readTimeout, 3000);
    }

    @Test
    public void remoteInstance_handle_deduplicatedHelpType() throws Exception {
        final Map<String, String> data = map("heapUsed", "119");
        final AppMeta appMeta = new AppMeta();
        App app = new App();
        app.code = "demo";
        app.models.add(model("jvm", field("heapUsed", "堆内存使用(MB)")));
        appMeta.setApp(app);
        final AppStubLocal stub = new AppStubLocal() {
            public AppMeta getAppMeta() {
                return appMeta;
            }

            public AppContext getAppContext() {
                return null;
            }

            public void invokeApp(RequestImpl request) {
                request.getResponse().data(data);
            }
        };
        final InstanceInfo info = new InstanceInfo();
        info.setId("inst-1");
        info.setHost("host-1");
        info.setPort(7900);
        info.setSslEnabled(false);

        PrometheusEndpoint endpoint = new PrometheusEndpoint();
        StubRequest request = new StubRequest();
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
                List<String> list = new ArrayList<>();
                list.add("demo");
                return list;
            }

            public AppStubLocal getLocalApp(String appCode) {
                return stub;
            }

            public List<String> getAllRemoteInstances() {
                List<String> list = new ArrayList<>();
                list.add("inst-1");
                return list;
            }

            public InstanceInfo getRemoteInstance(String instanceId) {
                return info;
            }

            public List<String> getAllRemoteApps(String instanceId) {
                return null;
            }

            public AppStubRemote getRemoteApp(String instanceId, String appCode) {
                return null;
            }
        });
        setField(endpoint, "httpClient", stubHttpClient(request, remoteText()));
        StubHttpResponse response = new StubHttpResponse();
        endpoint.handle(null, response);

        Assert.assertEquals(count(response.body, "# HELP qingzhou_heap_used"), 1);
        Assert.assertEquals(count(response.body, "# TYPE qingzhou_heap_used"), 1);
        Assert.assertEquals(count(response.body, "qingzhou_heap_used{"), 2);
    }

    private String invoke(Map<String, String> monitorData, Model... models) throws Exception {
        PrometheusEndpoint endpoint = new PrometheusEndpoint();
        setField(endpoint, "registry", localRegistry(monitorData, models));
        StubHttpResponse response = new StubHttpResponse();
        endpoint.handle(null, response);
        return response.body;
    }

    private Registry localRegistry(final Map<String, String> monitorData, final Model... models) {
        final AppMeta appMeta = new AppMeta();
        App app = new App();
        app.code = "demo";
        for (Model model : models) app.models.add(model);
        appMeta.setApp(app);
        final AppStubLocal stub = new AppStubLocal() {
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

    private Registry remoteRegistry(final String instanceId) {
        final InstanceInfo info = new InstanceInfo();
        info.setId(instanceId);
        info.setHost("host-1");
        info.setPort(7900);
        info.setSslEnabled(false);
        return new Registry() {
            public long getRegistryDataVersion() {
                return 0;
            }

            public AppStub getAppStub(String id, String appCode) {
                return null;
            }

            public InstanceInfo getLocalInstance() {
                return null;
            }

            public List<String> getAllLocalApps() {
                return new ArrayList<>();
            }

            public AppStubLocal getLocalApp(String appCode) {
                return null;
            }

            public List<String> getAllRemoteInstances() {
                List<String> list = new ArrayList<>();
                list.add(instanceId);
                return list;
            }

            public InstanceInfo getRemoteInstance(String id) {
                return info;
            }

            public List<String> getAllRemoteApps(String id) {
                return null;
            }

            public AppStubRemote getRemoteApp(String id, String appCode) {
                return null;
            }
        };
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

    private Response clientResponse(final int status, final byte[] body) {
        return new Response() {
            public int getStatus() {
                return status;
            }

            public byte[] getBody() {
                return body;
            }

            public void cancel() {
            }
        };
    }

    private HttpClient stubHttpClient(final StubRequest request, final String body) {
        return new HttpClient() {
            public Response send(Request req) {
                return clientResponse(200, body.getBytes(StandardCharsets.UTF_8));
            }

            public Response send(Request req, ResponseListener listener) {
                return send(req);
            }

            public Request newRequest(String url) {
                request.url = url;
                return request;
            }
        };
    }

    private String remoteText() {
        return "# HELP qingzhou_heap_used 堆内存使用(MB)\n# TYPE qingzhou_heap_used gauge\nqingzhou_heap_used{instance=\"inst-1\",app=\"demo\",model=\"jvm\",field=\"heapUsed\"} 63.0\n";
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

    private static class StubRequest implements Request {
        String url;
        int connectTimeout = -1;
        int readTimeout = -1;

        public Request method(HttpMethod method) {
            return this;
        }

        public Request header(String key, String val) {
            return this;
        }

        public Request headers(Map<String, String> headers) {
            return this;
        }

        public Request params(Map<String, String> params) {
            return this;
        }

        public Request body(byte[] body) {
            return this;
        }

        public Request files(Map<String, String> files) {
            return this;
        }

        public Request connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Request readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }
    }
}
