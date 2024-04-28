package qingzhou.app.instance;

import qingzhou.api.*;
import qingzhou.api.type.Showable;

@Model(code = "home", icon = "home", order = -1,
        entrance = "show",
        name = {"实例", "en:Instance"},
        info = {"展示当前实例的说明信息。",
                "en:Displays the description of the current instance."})
public class Home extends ModelBase implements Showable {
    @ModelField(
            name = {"Java 环境", "en:Java Env"},
            info = {"运行此 Qingzhou 实例的 Java 环境。", "en:The Java environment in which this Qingzhou instance is running."})
    public String javaHome;

    @ModelAction(
            name = {"首页", "en:Home"},
            info = {"展示实例的首页信息。", "en:Displays the homepage information of the instance."})
    public void show(Request request, Response response) throws Exception {
        Home home = new Home();
        home.javaHome = System.getProperty("java.home");
        response.addModelData(home);
    }
}
