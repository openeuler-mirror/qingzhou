package qingzhou.app.nodeagent;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Showable;
import qingzhou.framework.app.App;
import qingzhou.framework.util.IPUtil;

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

    @Override
    public ModelBase showModelData(String id) {
        NodeHome home = new NodeHome();
        home.javaHome = System.getProperty("java.home");
        home.localIps = String.join(",", IPUtil.getLocalIps());
        return home;
    }
}
