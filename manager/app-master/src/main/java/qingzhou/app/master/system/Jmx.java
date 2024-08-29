//package qingzhou.app.master.system;
//
//import qingzhou.api.FieldType;
//import qingzhou.api.Model;
//import qingzhou.api.ModelBase;
//import qingzhou.api.ModelField;
//import qingzhou.api.type.Updatable;
//import qingzhou.app.master.MasterApp;
//import qingzhou.config.Config;
//import qingzhou.engine.util.Utils;
//
//import java.util.Map;
//
//@Model(code = "jmx", icon = "exchange",
//        menu = "System", order = 5, entrance = "edit",
//        name = {"JMX", "en:JMX"}, hidden = true,
//        info = {"开启 JMX 接口服务后，客户端可以通过 java jmx 协议来管理 QingZhou。",
//                "en:After enabling the JMX interface service, the client can manage QingZhou through the java jmx protocol."})
//public class Jmx extends ModelBase implements Updatable {
//    @ModelField(
//            type = FieldType.bool,
//            name = {"启用", "en:Enabled"},
//            info = {"功能开关，配置是否开启 QingZhou 的 JMX 接口服务。",
//                    "en:Function switch, configure whether to enable QingZhou JMX interface service."})
//    public Boolean enabled = false;
//
//    @ModelField(
//            name = {"服务 IP", "en:Service IP"},
//            info = {"指定 JMX 监听服务绑定的 IP 地址。",
//                    "en:This configuration will override the RMI Server Hostname under Security Policy > Serialization Safety in the default instance."})
//    public String host = "127.0.0.1";
//
//    @ModelField(
//            port = true,
//            name = {"端口", "en:Port"},
//            info = {"指定 JMX 监听服务绑定的端口。", "en:Specifies the port to which the JMX listening service is bound."}
//    )
//    public Integer port = 7200;
//
//    @Override
//    public void updateData(Map<String, String> data) throws Exception {
//        Config config = MasterApp.getService(Config.class);
//        qingzhou.config.Jmx jmx = config.getConsole().getJmx();
//        Utils.setPropertiesToObj(jmx, data);
//        config.setJmx(jmx);
//
//        doJmxService(jmx); // 生效 jmx 服务
//    }
//
//    @Override
//    public Map<String, String> showData(String id) throws Exception {
//        Config config = MasterApp.getService(Config.class);
//        qingzhou.config.Jmx jmx = config.getConsole().getJmx();
//        return Utils.getPropertiesFromObj(jmx);
//    }
//
//    private void doJmxService(qingzhou.config.Jmx jmx) {
//        // todo: 1. 先停止 JMXServerHolder 服务
//
//        // todo: 2. 启动 jmx 服务
//        if (jmx.isEnabled()) {
//
//        }
//    }
//}
