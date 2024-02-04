package qingzhou.app.node;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.api.ShowModel;
import qingzhou.framework.util.IPUtil;
import qingzhou.framework.util.JDKUtil;

@Model(name = FrameworkContext.SYS_MODEL_HOME, icon = "home",
        entryAction = ShowModel.ACTION_NAME_SHOW,
        nameI18n = {"节点", "en:Node"},
        infoI18n = {"展示当前节点的说明信息。",
                "en:Displays the description of the current node."})
public class NodeHome extends ModelBase implements ShowModel {
    @ModelField(
            nameI18n = {"Java 版本", "en:Java Version"},
            infoI18n = {"此轻舟节点所使用的 Java 版本。", "en:The Java version used by this Qingzhou node."})
    public String javaVer;

    @ModelField(
            nameI18n = {"本地 IP", "en:Local Ip"},
            infoI18n = {"此轻舟节点的本地 IP 地址列表。", "en:A list of local IP addresses for this Qingzhou node."})
    public String localIps;

    @Override
    @ModelAction(name = ACTION_NAME_SHOW,
            icon = "info-sign", forwardToPage = "show",
            nameI18n = {"首页", "en:Home"},
            infoI18n = {"展示节点的首页信息。", "en:Displays the homepage information of the node."})
    public void show(Request request, Response response) throws Exception {
        NodeHome home = new NodeHome();
        home.javaVer = String.valueOf(JDKUtil.detectJavaVersion(JDKUtil.getJavaHome()));
        home.localIps = String.join(",", IPUtil.getLocalIps());
        response.addDataObject(home);
    }
}
