package qingzhou.app.node;

import qingzhou.framework.api.*;
import qingzhou.framework.util.JDKUtil;

@Model(name = "home", icon = "home", entryAction = "show",
        nameI18n = {"主页", "en:Main"},
        infoI18n = {"展示当前应用的说明信息。",
                "en:Displays the description of the current app."})
public class Home extends ModelBase implements ShowModel {
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
    public void show(Request request, Response response) throws Exception {
        Home home = new Home();
        home.info = "QingZhou（轻舟）节点";
        home.path = getAppContext().getDomain().getPath();
        home.javaHome = JDKUtil.getJavaHome();
        response.addDataObject(home);
    }
}
