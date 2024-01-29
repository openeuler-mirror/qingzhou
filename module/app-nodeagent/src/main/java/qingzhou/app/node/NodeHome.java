package qingzhou.app.node;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.api.ShowModel;
import qingzhou.framework.util.JDKUtil;

@Model(name = FrameworkContext.SYS_MODEL_HOME, icon = "home",
        entryAction = ShowModel.ACTION_NAME_SHOW,
        nameI18n = {"节点首页", "en:Node Main"},
        infoI18n = {"展示当前节点的说明信息。",
                "en:Displays the description of the current node."})
public class NodeHome extends ModelBase implements ShowModel {

    @ModelField(
            group = "product",
            nameI18n = {"产品信息", "en:Server Info"},
            infoI18n = {"当前轻舟产品的版本等信息。",
                    "en:Information such as the version of the current QingZhou product."})
    public String info;

    @ModelField(
            group = "product",
            nameI18n = {"节点目录", "en:Node Path"},
            infoI18n = {"当前运行的轻舟节点所对应的文件路径。", "en:The file path corresponding to the currently running QingZhou node."})
    public String path;

    @ModelField(
            group = "product",
            nameI18n = {"Java 环境", "en:Java Env"},
            infoI18n = {"当前运行的轻舟节点所使用的 Java 环境。", "en:The Java environment used by the currently running QingZhou node."})
    public String javaHome;

    @Override
    @ModelAction(name = ACTION_NAME_SHOW,
            icon = "info-sign", forwardToPage = "show",
            nameI18n = {"节点首页", "en:Node Home"},
            infoI18n = {"展示节点的首页信息。", "en:Displays the homepage information of the node."})
    public void show(Request request, Response response) throws Exception {
        NodeHome home = new NodeHome();
        home.info = "QingZhou（轻舟）节点";
        home.path = getAppContext().getDomain().getPath();
        home.javaHome = JDKUtil.getJavaHome();
        response.addDataObject(home);
    }
}
