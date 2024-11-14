package qingzhou.app.common;

import qingzhou.api.*;
import qingzhou.api.type.Show;

import java.util.HashMap;
import java.util.Map;

@Model(code = "home", icon = "home",
        entrance = "show",
        name = {"首页", "en:Home"},
        info = {"展示应用的默认首页信息。",
                "en:Displays the default app Home information."})
public class Home extends ModelBase implements Show {
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
            info = {"运行 Qingzhou 实例的 Java 环境。", "en:The Java environment in which Qingzhou instance is running."})
    public String javaHome;

    @ModelField(
            name = {"平台名称", "en:Platform Name"},
            info = {"Qingzhou 平台的名称。", "en:The name of Qingzhou platform."})
    public String platformName;

    @ModelField(
            name = {"平台版本", "en:Platform Version"},
            info = {"Qingzhou 平台的版本。", "en:This version of this Qingzhou platform."})
    public String platformVersion;

    @ModelAction(
            code = Show.ACTION_SHOW,
            name = {"首页", "en:Home"},
            info = {"进入此应用的默认首页。",
                    "en:Go to the default home of this app."})
    public void show(Request request) throws Exception {
        getAppContext().invokeSuperAction(request);
    }

    @Override
    public Map<String, String> showData(String id) {
        return new HashMap<String, String>() {{
            put("appName", getAppContext().getCurrentRequest().getApp());
            put("appDir", getAppContext().getAppDir().getAbsolutePath());
            put("javaHome", System.getProperty("java.home"));
            put("platformName", "Qingzhou（轻舟）");
            put("platformVersion", getAppContext().getPlatformVersion());
        }};
    }
}
