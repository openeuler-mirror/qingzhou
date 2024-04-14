package qingzhou.app.nodeagent;

import qingzhou.api.*;
import qingzhou.api.type.Showable;
import qingzhou.deployer.App;
import qingzhou.engine.util.IPUtil;

@Model(name = App.SYS_MODEL_HOME, icon = "home", menuOrder = -1,
        entryAction = App.SYS_ACTION_ENTRY_HOME,
        nameI18n = {"节点", "en:Node"},
        infoI18n = {"展示当前节点的说明信息。",
                "en:Displays the description of the current node."})
public class NodeHome extends ModelBase implements Showable {
    @ModelField(
            nameI18n = {"Java 环境", "en:Java Env"},
            infoI18n = {"运行此 Qingzhou 节点的 Java 环境。", "en:The Java environment in which this Qingzhou node is running."})
    public String javaHome;

    @ModelField(
            nameI18n = {"本地 IP", "en:Local Ip"},
            infoI18n = {"此 Qingzhou 节点的本地 IP 地址列表。", "en:A list of local IP addresses for this Qingzhou node."})
    public String localIps;

    @ModelAction(name = ACTION_NAME_SHOW,
            nameI18n = {"首页", "en:Home"},
            infoI18n = {"展示节点的首页信息。", "en:Displays the homepage information of the node."})
    @ActionView(icon = "info-sign", forwardTo = "show")
    public void show(Request request, Response response) throws Exception {
        NodeHome home = new NodeHome();
        home.javaHome = System.getProperty("java.home");
        home.localIps = String.join(",", IPUtil.getLocalIps());
        response.addModelData(home);
    }
}
