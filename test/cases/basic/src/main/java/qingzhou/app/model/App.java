package qingzhou.app.model;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Createable;

@Model(name = "app", icon = "leaf",
        nameI18n = {"应用测试模块", "en:App Test"},
        infoI18n = {"应用测试功能", "en:App Test"}
)
public class App extends ModelBase implements Createable {

    @ModelField(
            required = true,
            showToList = true,
            disableOnEdit = true,
            nameI18n = {"ID", "en:ID"},
            infoI18n = {"主键ID。", "en:ID"})
    public String id;

    @ModelField(
            required = true,
            showToList = true,
            nameI18n = {"应用名称", "en:App Name"},
            infoI18n = {"应用名称。", "en:App Name"})
    public String appName;

    @ModelField(
            required = true,
            showToList = true,
            nameI18n = {"应用图标", "en:App Logo"},
            infoI18n = {"应用图标。", "en:App Logo"})
    public String appLogo;

    @ModelField(
            required = true,
            showToList = true,
            nameI18n = {"应用版本", "en:App Version"},
            infoI18n = {"应用版本。", "en:App Version"})
    public String appVersion;

    @ModelField(
            required = true,
            showToList = true,
            nameI18n = {"应用详情", "en:App Detail"},
            infoI18n = {"应用详情。", "en:App Detail"})
    public String appDetail;

    @ModelField(
            required = true,
            showToList = true,
            nameI18n = {"当前安装版本", "en:Installed Version"},
            infoI18n = {"当前已安装应用版本。", "en:The installed app version"})
    public String installedVersion;

    public App() {
    }

    public App(String id, String appName, String appLogo, String appVersion, String appDetail, String installedVersion) {
        this.id = id;
        this.appName = appName;
        this.appLogo = appLogo;
        this.appVersion = appVersion;
        this.appDetail = appDetail;
        this.installedVersion = installedVersion;
    }

}
