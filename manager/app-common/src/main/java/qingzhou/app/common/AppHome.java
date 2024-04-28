package qingzhou.app.common;

import qingzhou.api.*;

@Model(code = "home", icon = "home",
        entrance = "show",
        order = -1,
        name = {"应用", "en:App"},
        info = {"展示当前应用的说明信息。",
                "en:Displays the description of the current app."})
public class AppHome extends ModelBase {
    @ModelField(
            name = {"应用名称", "en:App Name"},
            info = {"当前应用的名称。", "en:The name of the current app."})
    public String appName;

    @ModelField(
            name = {"Java 环境", "en:Java Env"},
            info = {"运行此应用的 Java 环境。", "en:The Java environment in which the app is running."})
    public String javaHome;

    @ModelAction(
            name = {"首页", "en:Home"},
            info = {"展示应用的首页信息。", "en:Displays the homepage information of the app."})
    public void show(Request request, Response response) throws Exception {
        AppHome home = new AppHome();
        home.appName = request.getApp();
        home.javaHome = System.getProperty("java.home");
        response.addModelData(home);
    }
}
