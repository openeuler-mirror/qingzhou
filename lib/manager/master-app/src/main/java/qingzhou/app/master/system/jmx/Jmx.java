package qingzhou.app.master.system.jmx;

import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Update;
import qingzhou.app.master.Main;
import qingzhou.app.master.ModelUtil;
import qingzhou.config.console.Console;
import qingzhou.core.console.JmxServiceAdapter;
import qingzhou.engine.ModuleContext;
import qingzhou.logger.Logger;

import java.util.Map;

@Model(code = "jmx", icon = "exchange",
        menu = Main.Setting, order = "4",
        entrance = Update.ACTION_EDIT,
        name = {"JMX", "en:JMX"},
        info = {"JMX 是 Java Management Extensions（Java管理扩展） 的缩写，它是 Java 平台上用于管理和监控应用程序、系统和网络资源的一种标准化的管理和监控框架。JMX 提供了一种标准的方式，通过这种方式，开发人员可以暴露应用程序中的各种管理和监控信息，然后可以使用 JMX 客户端工具或应用程序来访问和操作这些信息。开启 JMX 接口服务后，客户端可以通过 java jmx 协议来管理 Qingzhou 平台。",
                "en:JMX is an abbreviation for Java Management Extensions, which is a standardized management and monitoring framework for managing and monitoring applications, systems, and network resources on the Java platform. JMX provides a standard way for developers to expose various administrative and monitoring information in their applications, which can then be accessed and manipulated using JMX client tools or applications. After the JMX interface service is enabled, clients can manage the Qingzhou platform through the java jmx protocol."})
public class Jmx extends ModelBase implements Update {
    @ModelField(
            input_type = InputType.bool,
            name = {"启用", "en:Enabled"},
            info = {"功能开关，配置是否开启 Qingzhou 的 JMX 接口服务。",
                    "en:Function switch, configure whether to enable Qingzhou JMX interface service."})
    public Boolean enabled = false;

    @ModelField(display = "enabled=true",
            host = true,
            name = {"服务 IP", "en:Service IP"},
            info = {"指定 JMX 监听服务绑定的 IP 地址，为空表示绑定到 127.0.0.1。",
                    "en:Specifies the IP address to which the JMX listener service is bound, and if it is empty, it is bound to 127.0.0.1 ."})
    public String host = "127.0.0.1";

    @ModelField(display = "enabled=true",
            port = true,
            name = {"服务端口", "en:Service Port"},
            info = {"指定 JMX 监听服务绑定的端口。", "en:Specifies the port to which the JMX listening service is bound."})
    public Integer port = 7200;

    @Override
    public void start() throws Exception {
        Console console = Main.getConsole();
        if (console == null || !console.isEnabled()) return;
        qingzhou.config.console.Jmx jmx = console.getJmx();
        if (jmx == null || !jmx.isEnabled()) return;

        ServiceManager.getInstance().init(jmx);
        Main.getService(ModuleContext.class).registerService(JmxServiceAdapter.class, JmxServiceAdapterImpl.getInstance());
    }

    @Override
    public void stop() {
        try {
            ServiceManager.getInstance().destroy();
        } catch (Exception e) {
            getAppContext().getService(Logger.class).warn(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> editData(String id) {
        qingzhou.config.console.Jmx jmx = Main.getConsole().getJmx();
        if (jmx != null) {
            return ModelUtil.getPropertiesFromObj(jmx);
        } else {
            return ModelUtil.getPropertiesFromObj(new Jmx());
        }
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        qingzhou.config.console.Jmx jmx = Main.getConsole().getJmx();
        if (jmx == null) jmx = new qingzhou.config.console.Jmx();
        ModelUtil.setPropertiesToObj(jmx, data);
        doJmxService(jmx); // 生效 jmx 服务
        Main.getConfig().setJmx(jmx); // 最后没问题再写入配置文件
    }

    private void doJmxService(qingzhou.config.console.Jmx jmx) throws Exception {
        ServiceManager.getInstance().destroy();
        if (jmx.isEnabled()) {
            ServiceManager.getInstance().init(jmx);
        }
    }
}
