package qingzhou.app.common;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Show;

@Model(code = "home", icon = "home",
        entrance = "show",
        name = {"首页", "en:Home"},
        info = {"展示应用的默认首页信息。",
                "en:Displays the default app Home information."})
public class Home extends ModelBase {
    @ModelField(
            name = {"应用名称", "en:App Name"},
            info = {"应用的名称信息。", "en:The name information of the app."})
    public String appName;

    @ModelField(
            name = {"应用路径", "en:App Dir"},
            info = {"应用的路径信息。", "en:The path information of the app."})
    public String appDir;

    @ModelField(
            name = {"Java 环境", "en:Java Env"},
            info = {"运行 QingZhou 实例的 Java 环境。", "en:The Java environment in which QingZhou instance is running."})
    public String javaHome;

    @ModelAction(
            code = Show.ACTION_SHOW,
            page = "show",
            name = {"首页", "en:Home"},
            info = {"进入此应用的默认首页。",
                    "en:Go to the default home of this app."})
    public void show(Request request) throws Exception {
        Home home = new Home();
        home.appName = request.getApp();
        home.appDir = getAppContext().getAppDir().getAbsolutePath();
        home.javaHome = System.getProperty("java.home");
        request.getResponse().addModelData(home);
    }
}
