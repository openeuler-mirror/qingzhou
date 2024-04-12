package qingzhou.app.common;

import qingzhou.api.*;
import qingzhou.api.type.Showable;
import qingzhou.deployer.App;

@Model(name = App.SYS_MODEL_HOME, icon = "home",
        entryAction = App.SYS_ACTION_ENTRY_HOME,
        menuOrder = -1,
        nameI18n = {"应用", "en:App"},
        infoI18n = {"展示当前应用的说明信息。",
                "en:Displays the description of the current app."})
public class AppHome extends ModelBase {
    @ModelField(
            nameI18n = {"应用名称", "en:App Name"},
            infoI18n = {"当前应用的名称。", "en:The name of the current app."})
    public String appName;

    @ModelField(
            nameI18n = {"Java 环境", "en:Java Env"},
            infoI18n = {"运行此应用的 Java 环境。", "en:The Java environment in which the app is running."})
    public String javaHome;

    @ModelAction(name = Showable.ACTION_NAME_SHOW,
            icon = "info-sign", forwardTo = "show",
            nameI18n = {"首页", "en:Home"},
            infoI18n = {"展示应用的首页信息。", "en:Displays the homepage information of the app."})
    public void show(Request request, Response response) throws Exception {
        AppHome home = new AppHome();
        home.appName = request.getAppName();
        home.javaHome = System.getProperty("java.home");
        response.addModelData(home);
    }
}
