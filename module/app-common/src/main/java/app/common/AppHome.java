package app.common;

import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.api.ShowModel;
import qingzhou.framework.util.JDKUtil;

@Model(name = AppHome.modelName, icon = "home", entryAction = "show",
        nameI18n = {"主页", "en:Main"},
        infoI18n = {"展示当前应用的说明信息。",
                "en:Displays the description of the current app."})
public class AppHome extends ModelBase implements ShowModel {
    public static final String modelName = "apphome";

    @ModelField(
            group = "product",
            nameI18n = {"应用名称", "en:App Name"},
            infoI18n = {"应用名称。",
                    "en:App Name."})
    public String appName;

    @ModelField(
            group = "product",
            nameI18n = {"产品信息", "en:Server Info"},
            infoI18n = {"当前轻舟产品的版本等信息。",
                    "en:Information such as the version of the current QingZhou product."})
    public String info;

    @ModelField(
            group = "product",
            nameI18n = {"节点目录", "en:Node Path"},
            infoI18n = {"当前运行的轻舟节点所对应的文件路径。", "en:The file path corresponding to the currently running QingZhou common."})
    public String path;

    @ModelField(
            group = "product",
            nameI18n = {"Java 环境", "en:Java Env"},
            infoI18n = {"当前运行的轻舟节点所使用的 Java 环境。", "en:The Java environment used by the currently running QingZhou common."})
    public String javaHome;

    @Override
    @ModelAction(name = ACTION_NAME_SHOW,
            icon = "info-sign", forwardToPage = "home",
            nameI18n = {"应用首页", "en:App Home"},
            infoI18n = {"展示应用的首页信息。", "en:Displays the homepage information of the app."})
    public void show(Request request, Response response) throws Exception {
        AppHome home = new AppHome();
        home.appName = request.getAppName();
        home.info = "QingZhou（轻舟）节点";
        home.path = getAppContext().getDomain().getPath();
        home.javaHome = JDKUtil.getJavaHome();
        response.addDataObject(home);
    }
}
