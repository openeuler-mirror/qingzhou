package qingzhou.app.instance;

import qingzhou.api.*;
import qingzhou.api.type.Showable;

import java.util.Map;

@Model(code = "home", icon = "home", order = -1,
        entrance = "show",
        name = {"实例", "en:Instance"},
        info = {"展示当前实例的说明信息。",
                "en:Displays the description of the current instance."})
public class Home extends ModelBase implements Showable {
    @ModelField(
            name = {"Java 环境", "en:Java Env"},
            info = {"运行 QingZhou 实例的 Java 环境。", "en:The Java environment in which QingZhou instance is running."})
    public String javaHome;

    @ModelAction(
            name = {"首页", "en:Home"},
            info = {"展示实例的首页信息。", "en:Displays the homepage information of the instance."})
    public void show(Request request, Response response) throws Exception {
        Home home = new Home();
        home.javaHome = System.getProperty("java.home");
        response.addModelData(home);
    }

    @Override
    public Map<String, String> showData(String id) {
        throw new IllegalArgumentException("覆写了 show，不应该进入这里！");
    }
}
