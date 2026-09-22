package qingzhou.agent;

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
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.impl.JsonImpl;
import qingzhou.registry.AppStub;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.AppStubRemote;
import qingzhou.registry.Registry;

public class MonitorTest {

    @Test
    public void collect_handle_returnsStructuredNeutralData() throws Exception {
        Monitor monitor = new Monitor();
        Map<String, String> data = new HashMap<>();
        data.put("heapUsed", "128");
        setField(monitor, "registry", stubRegistry("demo", data, model("jvm", field("heapUsed", FieldType.monitor))));
        setField(monitor, "json", new JsonImpl());

        Map<String, Object> result = monitor.collect();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("demo"));
        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> appData = (Map<String, Map<String, String>>) result.get("demo");
        Assert.assertTrue(appData.containsKey("jvm"));
        Assert.assertEquals(appData.get("jvm").get("heapUsed"), "128");
    }

    @Test
    public void handle_http_returnsValidJson() throws Exception {
        Monitor monitor = new Monitor();
        Map<String, String> data = new HashMap<>();
        data.put("threadCount", "32");
        setField(monitor, "registry", stubRegistry("demo", data, model("jvm", field("threadCount", FieldType.monitor))));
        JsonImpl json = new JsonImpl();
        json.init();
        setField(monitor, "json", json);

        StubHttpResponse response = new StubHttpResponse();
        monitor.handle(null, response);

        Assert.assertNotNull(response.body);
        Assert.assertEquals(response.contentType, "application/json; charset=utf-8");
        Assert.assertTrue(response.body.contains("\"demo\""));
        Assert.assertTrue(response.body.contains("\"jvm\""));
        Assert.assertTrue(response.body.contains("\"threadCount\":\"32\""));
        Assert.assertFalse(response.body.contains("# HELP"));
        Assert.assertFalse(response.body.contains("# TYPE"));
    }

    @Test
    public void nonMonitorField_collect_ignored() throws Exception {
        Monitor monitor = new Monitor();
        Map<String, String> data = new HashMap<>();
        data.put("normalField", "value");
        setField(monitor, "registry", stubRegistry("demo", data, model("jvm", field("normalField", FieldType.form))));
        setField(monitor, "json", new JsonImpl());

        Map<String, Object> result = monitor.collect();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void emptyApps_collect_returnsEmptyMap() throws Exception {
        Monitor monitor = new Monitor();
        setField(monitor, "registry", new Registry() {
            public long getRegistryDataVersion() { return 0; }
            public AppStub getAppStub(String instanceId, String appCode) { return null; }
            public InstanceInfo getLocalInstance() { return null; }
            public List<String> getAllLocalApps() { return Collections.emptyList(); }
            public AppStubLocal getLocalApp(String appCode) { return null; }
            public List<String> getAllRemoteInstances() { return Collections.emptyList(); }
            public InstanceInfo getRemoteInstance(String instanceId) { return null; }
            public List<String> getAllRemoteApps(String instanceId) { return Collections.emptyList(); }
            public AppStubRemote getRemoteApp(String instanceId, String appCode) { return null; }
        });
        Map<String, Object> result = monitor.collect();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void modelWithoutMonitorAction_collect_ignored() throws Exception {
        Monitor monitor = new Monitor();
        Map<String, String> data = new HashMap<>();
        data.put("heapUsed", "100");
        Model model = new Model();
        model.code = "jvm";
        model.fields.add(field("heapUsed", FieldType.monitor));
        setField(monitor, "registry", stubRegistry("demo", data, model));
        Map<String, Object> result = monitor.collect();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void multipleAppsAndModels_collect_structuredHierarchy() throws Exception {
        Monitor monitor = new Monitor();
        Map<String, String> jvmData = new HashMap<>();
        jvmData.put("heap", "50");
        Map<String, String> osData = new HashMap<>();
        osData.put("load", "1.5");

        AppMeta appMeta1 = new AppMeta();
        App app1 = new App();
        app1.code = "app1";
        app1.models.add(model("jvm", field("heap", FieldType.monitor)));
        app1.models.add(model("os", field("load", FieldType.monitor)));
        appMeta1.setApp(app1);

        AppStubLocal stub1 = new AppStubLocal() {
            public AppMeta getAppMeta() { return appMeta1; }
            public AppContext getAppContext() { return null; }
            public void invokeApp(RequestImpl request) {
                if ("jvm".equals(request.getModel())) request.getResponse().data(jvmData);
                if ("os".equals(request.getModel())) request.getResponse().data(osData);
            }
        };

        setField(monitor, "registry", new Registry() {
            public long getRegistryDataVersion() { return 0; }
            public AppStub getAppStub(String instanceId, String appCode) { return null; }
            public InstanceInfo getLocalInstance() { return null; }
            public List<String> getAllLocalApps() { return Collections.singletonList("app1"); }
            public AppStubLocal getLocalApp(String appCode) { return stub1; }
            public List<String> getAllRemoteInstances() { return Collections.emptyList(); }
            public InstanceInfo getRemoteInstance(String instanceId) { return null; }
            public List<String> getAllRemoteApps(String instanceId) { return Collections.emptyList(); }
            public AppStubRemote getRemoteApp(String instanceId, String appCode) { return null; }
        });

        Map<String, Object> result = monitor.collect();
        Assert.assertEquals(result.size(), 1);
        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> app1Map = (Map<String, Map<String, String>>) result.get("app1");
        Assert.assertEquals(app1Map.size(), 2);
        Assert.assertEquals(app1Map.get("jvm").get("heap"), "50");
        Assert.assertEquals(app1Map.get("os").get("load"), "1.5");
    }

    private Registry stubRegistry(String appCode, Map<String, String> monitorData, Model... models) {
        AppMeta appMeta = new AppMeta();
        App app = new App();
        app.code = appCode;
        for (Model model : models) app.models.add(model);
        appMeta.setApp(app);

        AppStubLocal stub = new AppStubLocal() {
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
                return Collections.singletonList(appCode);
            }

            public AppStubLocal getLocalApp(String appCode) {
                return stub;
            }

            public List<String> getAllRemoteInstances() {
                return Collections.emptyList();
            }

            public InstanceInfo getRemoteInstance(String instanceId) {
                return null;
            }

            public List<String> getAllRemoteApps(String instanceId) {
                return Collections.emptyList();
            }

            public AppStubRemote getRemoteApp(String instanceId, String appCode) {
                return null;
            }
        };
    }

    private Model model(String code, ModelField... fields) {
        Model model = new Model();
        model.code = code;
        ModelAction action = new ModelAction();
        action.code = qingzhou.api.action.Monitor.ACTION_CODE_MONITOR;
        model.actions.add(action);
        for (ModelField field : fields) model.fields.add(field);
        return model;
    }

    private ModelField field(String code, FieldType type) {
        ModelField field = new ModelField();
        field.code = code;
        field.name = new String[]{code};
        field.field_type = type;
        return field;
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
