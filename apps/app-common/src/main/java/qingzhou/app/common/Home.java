package qingzhou.app.common;

import qingzhou.api.*;

@Model(code = "home", icon = "home",
        entrance = "show",
        name = {"首页", "en:Home"},
        info = {"展示应用的默认首页信息。",
                "en:Displays the default app Home information."})
public class Home extends ModelBase {
    @ModelField(
            name = {"应用路径", "en:App Dir"},
            info = {"应用的路径信息。", "en:The path information of the app."})
    public String appDir;

    @ModelField(
            name = {"Java 环境", "en:Java Env"},
            info = {"运行 QingZhou 实例的 Java 环境。", "en:The Java environment in which QingZhou instance is running."})
    public String javaHome;

    @ModelAction(
            code = "show",
            name = {"首页", "en:Home"},
            info = {"进入此应用的默认首页。",
                    "en:Go to the default home of this app."})
    public void show(Request request) throws Exception {
        Home home = new Home();
        home.appDir = appContext.getAppDir().getAbsolutePath();
        home.javaHome = System.getProperty("java.home");
        request.getResponse().addModelData(home);
    }
}
