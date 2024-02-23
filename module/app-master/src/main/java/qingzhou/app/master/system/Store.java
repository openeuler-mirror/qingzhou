package qingzhou.app.master.system;

import qingzhou.api.*;

@Model(name = "store", icon = "database",
        menuName = "System", menuOrder = 2,
        nameI18n = {"存储管理", "en:Data Store"},
        infoI18n = {"调整轻舟数据、文件的存储方式，以适应云原生等使用环境。",
                "en:Adjust the storage method of Qingzhou's data and files to adapt to the use environment such as cloud native."})
public class Store extends ModelBase implements EditModel {
    // TODO 支持云原生，以不同的 group 来显示

    public boolean configEnabled = false; // TODO: 配置中心，以更换 ConfigManager 的实现类方式来实现
    public boolean logEnabled = false; // TODO: 推送到ES，连接 fluentd、opensearch。
    public boolean monitorEnabled = false; // TODO: 对接普罗米修斯
    public boolean appEnabled = false; // TODO: 连接应用仓库，从中拉取需要的应用文件
    public boolean serviceEnabled = false; // TODO: 注册和发现轻舟的运行时
    public boolean ssoEnabled = false; // TODO: 支持云上单点登录统一管理

    @Override
    public Groups group() {
        return Groups.of(
                Group.of("config", new String[]{"配置中心", "en:Config"}),
                Group.of("log", new String[]{"日志收集", "en:Log"}),
                Group.of("monitor", new String[]{"监视告警", "en:Monitor"}),
                Group.of("app", new String[]{"应用仓库", "en:App"}),
                Group.of("service", new String[]{"服务发现", "en:Service"}),
                Group.of("sso", new String[]{"单点登录", "en:SSO"})
        );
    }
}
