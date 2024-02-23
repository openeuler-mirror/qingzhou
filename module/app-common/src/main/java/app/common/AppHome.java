package app.common;

import qingzhou.api.*;
import qingzhou.framework.util.JDKUtil;

@Model(name = qingzhou.app.App.SYS_MODEL_HOME, icon = "home",
        entryAction = ShowModel.ACTION_NAME_SHOW,
        menuOrder = -1,
        nameI18n = {"应用", "en:App"},
        infoI18n = {"展示当前应用的说明信息。",
                "en:Displays the description of the current app."})
public class AppHome extends ModelBase implements ShowModel {
    @ModelField(
            nameI18n = {"应用名称", "en:App Name"},
            infoI18n = {"当前应用的名称。", "en:The name of the current app."})
    public String appName;

    @ModelField(
            nameI18n = {"平台名称", "en:Platform Name"},
            infoI18n = {"运行此应用的平台名称。", "en:The name of the platform on which the app is running."})
    public String platform;

    @ModelField(
            nameI18n = {"平台版本", "en:Platform Version"},
            infoI18n = {"运行此应用的平台版本。", "en:The version of the platform on which the app is running."})
    public String version;

    @ModelField(
            nameI18n = {"Java 环境", "en:Java Env"},
            infoI18n = {"运行此应用的 Java 环境。", "en:The Java environment in which the app is running."})
    public String javaHome;

    @Override
    @ModelAction(name = ACTION_NAME_SHOW,
            icon = "info-sign", forwardToPage = "show",
            nameI18n = {"首页", "en:Home"},
            infoI18n = {"展示应用的首页信息。", "en:Displays the homepage information of the app."})
    public void show(Request request, Response response) throws Exception {
        AppHome home = new AppHome();
        home.appName = request.getAppName();
        home.platform = getAppContext().getPlatformName();
        home.version = getAppContext().getPlatformVersion();
        home.javaHome = JDKUtil.getJavaHome();
        response.addDataObject(home);
    }
}
