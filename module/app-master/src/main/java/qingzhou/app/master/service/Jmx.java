package qingzhou.app.master.service;

import qingzhou.api.*;
import qingzhou.api.type.Editable;
import qingzhou.app.master.Main;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Model(name = "jmx", icon = "code",
        menuName = "Service", menuOrder = 3,
        entryAction = Editable.ACTION_NAME_EDIT,
        nameI18n = {"JMX", "en:JMX"},
        infoI18n = {"开启 JMX 接口服务后，客户端可以通过 java jmx 协议来管理 Qingzhou。",
                "en:After enabling the JMX interface service, the client can manage Qingzhou through the java jmx protocol."})
public class Jmx extends ModelBase implements Editable {
    private static final String DEFAULT_ID = "jmx_0";
    private final String tagName = "jmx";

    @ModelField(
            type = FieldType.bool,
            nameI18n = {"启用", "en:Enabled"},
            infoI18n = {"功能开关，配置是否开启 Qingzhou 的 JMX 接口服务。",
                    "en:Function switch, configure whether to enable Qingzhou JMX interface service."})
    public Boolean enabled = false;

    @ModelField(
            effectiveWhen = "enabled=true",
            isIpOrHostname = true,
            nameI18n = {"服务 IP", "en:Service IP"},
            infoI18n = {"指定 JMX 监听服务绑定的 IP 地址。此配置将覆盖默认实例中“安全策略” > “序列化安全”下的 RMI 服务主机名。", "en:This configuration will override the RMI Server Hostname under Security Policy > Serialization Safety in the default instance."})
    public String ip = "127.0.0.1";

    @ModelField(
            effectiveWhen = "enabled=true",
            type = FieldType.number,
            nameI18n = {"端口", "en:Port"},
            infoI18n = {"指定 JMX 监听服务绑定的端口。", "en:Specifies the port to which the JMX listening service is bound."},
            isPort = true)
    public Integer port = 7200;

    @ModelAction(name = Editable.ACTION_NAME_UPDATE,
            icon = "save",
            nameI18n = {"更新", "en:Update"},
            infoI18n = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
        Map<String, String> oldProperties = getDataStore().getDataById(request.getModelName(), DEFAULT_ID);

        Map<String, String> properties = Main.prepareParameters(request, getAppContext());
        getDataStore().updateDataById(request.getModelName(), DEFAULT_ID, properties);

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
            getDataStore().updateDataById(request.getModelName(), DEFAULT_ID, oldProperties);
            // ConsoleXml.getInstance().consoleXmlChanged();
            throw e;
        }
    }

    @ModelAction(name = Editable.ACTION_NAME_EDIT,
            icon = "edit", forwardToPage = "form",
            nameI18n = {"编辑", "en:Edit"},
            infoI18n = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
        Map<String, String> data = getDataStore().getDataById(request.getModelName(), DEFAULT_ID);
        if (data == null || data.isEmpty()) {
            response.addModelData(new Jmx());
        } else {
            response.addData(data);
        }
    }
}
