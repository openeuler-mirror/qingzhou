package qingzhou.app.master.service;

import qingzhou.api.*;
import qingzhou.api.type.Editable;
import qingzhou.app.master.MasterApp;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Model(code = "jmx", icon = "code",
        menu = "Service", order = 3,
        entrance = Editable.ACTION_NAME_EDIT,
        name = {"JMX", "en:JMX"},
        info = {"开启 JMX 接口服务后，客户端可以通过 java jmx 协议来管理 Qingzhou。",
                "en:After enabling the JMX interface service, the client can manage Qingzhou through the java jmx protocol."})
public class Jmx extends ModelBase implements Editable {
    private static final String DEFAULT_ID = "jmx_0";
    private final String tagName = "jmx";

    @ModelField(
            name = {"启用", "en:Enabled"},
            info = {"功能开关，配置是否开启 Qingzhou 的 JMX 接口服务。",
                    "en:Function switch, configure whether to enable Qingzhou JMX interface service."})
    public Boolean enabled = false;

    @ModelField(
            name = {"服务 IP", "en:Service IP"},
            info = {"指定 JMX 监听服务绑定的 IP 地址。此配置将覆盖默认实例中“安全策略” > “序列化安全”下的 RMI 服务主机名。", "en:This configuration will override the RMI Server Hostname under Security Policy > Serialization Safety in the default instance."})
    public String ip = "127.0.0.1";

    @ModelField(
            name = {"端口", "en:Port"},
            info = {"指定 JMX 监听服务绑定的端口。", "en:Specifies the port to which the JMX listening service is bound."}
    )
    public Integer port = 7200;

    @ModelAction(
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
        Map<String, String> oldProperties = getDataStore().getDataById(request.getModel(), DEFAULT_ID);

        Map<String, String> properties = request.getParameters();
        getDataStore().updateDataById(request.getModel(), DEFAULT_ID, properties);

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
            getDataStore().updateDataById(request.getModel(), DEFAULT_ID, oldProperties);
            // ConsoleXml.getInstance().consoleXmlChanged();
            throw e;
        }
    }

    @ModelAction(
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
        Map<String, String> data = getDataStore().getDataById(request.getModel(), DEFAULT_ID);
        if (data == null) {
            response.addModelData(new Jmx());
        } else {
            response.addData(data);
        }
    }
}
