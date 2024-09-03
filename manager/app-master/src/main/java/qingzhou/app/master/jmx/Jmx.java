package qingzhou.app.master.jmx;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Updatable;
import qingzhou.app.master.Main;
import qingzhou.config.Config;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.JmxServiceAdapter;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;
import qingzhou.logger.Logger;

import java.util.Map;

@Model(code = "jmx", icon = "exchange",
        menu = "System", order = 4,
        entrance = DeployerConstants.ACTION_EDIT,
        name = {"JMX", "en:JMX"},
        info = {"开启 JMX 接口服务后，客户端可以通过 java jmx 协议来管理 QingZhou。",
                "en:After enabling the JMX interface service, the client can manage QingZhou through the java jmx protocol."})
public class Jmx extends ModelBase implements Updatable {
    @ModelField(
            type = FieldType.bool,
            name = {"启用", "en:Enabled"},
            info = {"功能开关，配置是否开启 QingZhou 的 JMX 接口服务。",
                    "en:Function switch, configure whether to enable QingZhou JMX interface service."})
    public Boolean enabled = false;

    @ModelField(
            show = "enabled=true",
            host = true,
            name = {"服务 IP", "en:Service IP"},
            info = {"指定 JMX 监听服务绑定的 IP 地址，为空表示绑定到所有 IP。",
                    "en:Specifies the IP address to which the JMX listener service is bound, and if it is empty, it is bound to all IPs."})
    public String host;

    @ModelField(
            show = "enabled=true",
            port = true,
            name = {"服务端口", "en:Service Port"},
            info = {"指定 JMX 监听服务绑定的端口。", "en:Specifies the port to which the JMX listening service is bound."}
    )
    public Integer port = 7200;

    @Override
    public void start() {
        try {
            qingzhou.config.Jmx jmx = Main.getService(Config.class).getConsole().getJmx();
            if (jmx.isEnabled()) {
                ServiceManager.getInstance().init(jmx);
            }
        } catch (Exception e) {
            appContext.getService(Logger.class).error(e.getMessage(), e);
        }

        Main.getService(ModuleContext.class).registerService(JmxServiceAdapter.class, JmxServiceAdapterImpl.getInstance());
    }

    @Override
    public void stop() {
        try {
            ServiceManager.getInstance().destroy();
        } catch (Exception e) {
            appContext.getService(Logger.class).error(e.getMessage(), e);
        }
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        Config config = Main.getService(Config.class);
        qingzhou.config.Jmx jmx = config.getConsole().getJmx();
        Utils.setPropertiesToObj(jmx, data);
        doJmxService(jmx); // 生效 jmx 服务
        config.setJmx(jmx); // 最后没问题再写入配置文件
    }

    @Override
    public Map<String, String> showData(String id) throws Exception {
        Config config = Main.getService(Config.class);
        qingzhou.config.Jmx jmx = config.getConsole().getJmx();
        return Utils.getPropertiesFromObj(jmx);
    }

    private void doJmxService(qingzhou.config.Jmx jmx) throws Exception {
        ServiceManager.getInstance().destroy();
        if (jmx.isEnabled()) {
            ServiceManager.getInstance().init(jmx);
        }
    }
}
