package app.common;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.api.ShowModel;
import qingzhou.framework.util.JDKUtil;

@Model(name = FrameworkContext.SYS_MODEL_Home, icon = "home", entryAction = "show",
        nameI18n = {"应用首页", "en:App Main"},
        infoI18n = {"展示当前应用的说明信息。",
                "en:Displays the description of the current app."})
public class AppHome extends ModelBase implements ShowModel {
    @ModelField(
            group = "product",
            nameI18n = {"应用名称", "en:App Name"},
            infoI18n = {"应用名称。",
                    "en:App Name."})
    public String appName;

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
        home.path = getAppContext().getDomain().getPath();
        home.javaHome = JDKUtil.getJavaHome();
        response.addDataObject(home);
    }
}
