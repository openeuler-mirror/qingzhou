package qingzhou.app.master.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import qingzhou.api.DataStore;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Editable;
import qingzhou.app.master.MasterApp;
import qingzhou.engine.util.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Model(code = "jmx", icon = "exchange",
        menu = "System", order = 3,
        entrance = Editable.ACTION_NAME_EDIT,
        name = {"JMX", "en:JMX"}, hidden = true,
        info = {"开启 JMX 接口服务后，客户端可以通过 java jmx 协议来管理 Qingzhou。",
                "en:After enabling the JMX interface service, the client can manage Qingzhou through the java jmx protocol."})
public class Jmx extends ModelBase implements Editable {
    private static final String DEFAULT_ID = "jmx_0";

    @ModelField(
            type = FieldType.bool,
            name = {"启用", "en:Enabled"},
            info = {"功能开关，配置是否开启 Qingzhou 的 JMX 接口服务。",
                    "en:Function switch, configure whether to enable Qingzhou JMX interface service."})
    public Boolean enabled = false;

    @ModelField(
            name = {"服务 IP", "en:Service IP"},
            info = {"指定 JMX 监听服务绑定的 IP 地址。此配置将覆盖默认实例中“安全策略” > “序列化安全”下的 RMI 服务主机名。", "en:This configuration will override the RMI Server Hostname under Security Policy > Serialization Safety in the default instance."})
    public String host = "127.0.0.1";

    @ModelField(
            port = true,
            name = {"端口", "en:Port"},
            info = {"指定 JMX 监听服务绑定的端口。", "en:Specifies the port to which the JMX listening service is bound."}
    )
    public Integer port = 7200;

    @ModelAction(
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
        Map<String, String> oldProperties = getDataStore().getDataById(DEFAULT_ID);

        Map<String, String> properties = request.getParameters();
        getDataStore().updateDataById(DEFAULT_ID, properties);

        // ConsoleXml.getInstance().consoleXmlChanged();
        try {
            if (Boolean.parseBoolean(properties.get("enabled"))) {
                if (oldProperties != null && Boolean.parseBoolean(oldProperties.get("enabled"))) {
                    // JMXServerHolder.getInstance().destroy();
                    if (oldProperties.get("port").equals(properties.get("port"))) {
                        try {
                            // 监听端口未变化时，销毁后立即初始化可能会存在端口还在使用的情况
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                // JMXServerHolder.getInstance().init();
            } else {
                // JMXServerHolder.getInstance().destroy();
            }
        } catch (Exception e) {
            getDataStore().updateDataById(DEFAULT_ID, oldProperties);
            // ConsoleXml.getInstance().consoleXmlChanged();
            throw e;
        }
    }

    @Override
    public DataStore getDataStore() {
        return jmxDataStore;
    }

    private final JmxDataStore jmxDataStore = new JmxDataStore();

    private static class JmxDataStore implements DataStore {
        public JmxDataStore() {
        }

        private String configFile;

        private static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

        @Override
        public List<Map<String, String>> getAllData() throws Exception {
            JsonObject jsonObject = readJsonFile();
            if (jsonObject != null) {
                JsonObject jmxObject = getJmxObject(jsonObject);
                List<Map<String, String>> list = new ArrayList<>();
                Map<String, String> map = new HashMap<>();
                for (String key : jmxObject.keySet()) {
                    JsonElement element = jmxObject.get(key);
                    if (!element.isJsonNull()) {
                        map.put(key, element.getAsString());
                    } else {
                        map.put(key, "");
                    }
                }
                list.add(map);

                return list;
            }

            return null;
        }

        @Override
        public void addData(String id, Map<String, String> data) throws Exception {
            throw new RuntimeException("No Support.");
        }

        @Override
        public void updateDataById(String id, Map<String, String> data) throws Exception {
            JsonObject jsonObject = readJsonFile();

            if (jsonObject != null) {
                JsonObject jmxObject = getJmxObject(jsonObject);
                for (String key : data.keySet()) {
                    jmxObject.addProperty(key, data.get(key));
                }

                writeJsonFile(jsonObject);
            }
        }

        @Override
        public void deleteDataById(String id) throws Exception {
            throw new RuntimeException("No Support.");
        }

        private JsonObject getJmxObject(JsonObject jsonObject) {
            return jsonObject.getAsJsonObject("module").getAsJsonObject("console").getAsJsonObject("jmx");
        }

        private JsonObject readJsonFile() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(getConfigFile())), StandardCharsets.UTF_8))) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        private void writeJsonFile(JsonObject jsonObject) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Paths.get(getConfigFile())), StandardCharsets.UTF_8))) {
                gson.toJson(jsonObject, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String getConfigFile() throws IOException {
            if (configFile == null || configFile.isEmpty()) {
                configFile = Utils.newFile(MasterApp.getInstanceDir(), "qingzhou.json").getCanonicalPath();
            }

            return configFile;
        }
    }
}
