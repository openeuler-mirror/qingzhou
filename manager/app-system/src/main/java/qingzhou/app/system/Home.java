package qingzhou.app.system;

import qingzhou.api.*;
import qingzhou.deployer.DeployerConstants;

@Model(code = DeployerConstants.MODEL_HOME, icon = "home",
        entrance = DeployerConstants.ACTION_SHOW,
        hidden = true,
        name = {"首页", "en:Home"},
        info = {"展示应用的默认首页信息。",
                "en:Displays the default app Home information."})
public class Home extends ModelBase {
    @ModelField(
            name = {"应用名称", "en:App Name"},
            info = {"应用的名称。", "en:The name of this app."})
    public String appName;

    @ModelField(
            name = {"Java 环境", "en:Java Env"},
            info = {"运行 QingZhou 实例的 Java 环境。", "en:The Java environment in which QingZhou instance is running."})
    public String javaHome;

    @ModelAction(
            code = DeployerConstants.ACTION_SHOW,
            name = {"首页", "en:Home"},
            info = {"进入此应用的默认首页。",
                    "en:Go to the default home of this app."})
    public void show(Request request) throws Exception {
        Home home = new Home();
        home.appName = request.getId();
        home.javaHome = System.getProperty("java.home");
        request.getResponse().addModelData(home);
    }
}
