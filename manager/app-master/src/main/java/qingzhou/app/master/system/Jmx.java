package qingzhou.app.master.system;

import qingzhou.api.*;
import qingzhou.api.type.Updatable;
import qingzhou.app.master.MasterApp;
import qingzhou.config.Config;
import qingzhou.engine.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Model(code = "jmx", icon = "exchange",
        menu = "System", order = 5, entrance = "edit",
        name = {"JMX", "en:JMX"}, hidden = true,
        info = {"开启 JMX 接口服务后，客户端可以通过 java jmx 协议来管理 QingZhou。",
                "en:After enabling the JMX interface service, the client can manage QingZhou through the java jmx protocol."})
public class Jmx extends ModelBase implements Updatable {
    private static final String DEFAULT_ID = "jmx_0";

    @ModelField(
            type = FieldType.bool,
            name = {"启用", "en:Enabled"},
            info = {"功能开关，配置是否开启 QingZhou 的 JMX 接口服务。",
                    "en:Function switch, configure whether to enable QingZhou JMX interface service."})
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

    private static class JmxDataStore implements DataStore {
        @Override
        public List<Map<String, String>> getAllData() throws Exception {
            qingzhou.config.Jmx jmx = MasterApp.getService(Config.class).getConsole().getJmx();
            Map<String, String> propertiesFromObj = Utils.getPropertiesFromObj(jmx);
            List<Map<String, String>> list = new ArrayList<>();
            list.add(propertiesFromObj);
            return list;
        }

        @Override
        public void addData(String id, Map<String, String> data) throws Exception {
            throw new RuntimeException("No Support.");
        }

        @Override
        public void updateDataById(String id, Map<String, String> data) throws Exception {
            Config config = MasterApp.getService(Config.class);
            qingzhou.config.Jmx jmx = config.getConsole().getJmx();
            Utils.setPropertiesToObj(jmx, data);
            config.setJmx(jmx);
        }

        @Override
        public void deleteDataById(String id) {
            throw new RuntimeException("No Support.");
        }
    }
}
