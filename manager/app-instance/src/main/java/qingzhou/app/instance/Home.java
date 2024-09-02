package qingzhou.app.instance;

import qingzhou.api.*;
import qingzhou.deployer.DeployerConstants;

@Model(code = DeployerConstants.HOME_MODEL, icon = "home",
        order = -1,
        entrance = DeployerConstants.SHOW_ACTION,
        name = {"实例", "en:Instance"},
        info = {"展示当前实例的说明信息。",
                "en:Displays the description of the current instance."})
public class Home extends ModelBase {
    @ModelField(
            name = {"Java 环境", "en:Java Env"},
            info = {"运行 QingZhou 实例的 Java 环境。", "en:The Java environment in which QingZhou instance is running."})
    public String javaHome;

    @ModelAction(
            code = DeployerConstants.SHOW_ACTION,
            name = {"首页", "en:Home"},
            info = {"展示实例的首页信息。", "en:Displays the homepage information of the instance."})
    public void show(Request request) throws Exception {
        Home home = new Home();
        home.javaHome = System.getProperty("java.home");
        request.getResponse().addModelData(home);
    }
}
