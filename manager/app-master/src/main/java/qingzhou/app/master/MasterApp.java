package qingzhou.app.master;

import qingzhou.api.AppContext;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;

@qingzhou.api.App
public class MasterApp extends QingzhouSystemApp {
    private static MasterApp masterApp;

    @Override
    public void start(AppContext appContext) {
        masterApp = this;

        appContext.addI18n("validator.master.system", new String[]{"为保障系统安全可用，请勿修改此配置", "en:To ensure the security and availability of the system, do not modify this configuration"});
        appContext.addI18n("client.trusted.not", new String[]{"该操作仅限于在服务器本机或受信任的IP上执行，受信任IP的设置方式请参考产品手册", "en:This operation can only be performed on the local server or on a trusted IP. Please refer to the product manual for the setting method of the trusted IP"});

        appContext.addMenu("Service", new String[]{"服务管理", "en:Service"}, "th-large", 1);
        appContext.addMenu("System", new String[]{"系统管理", "en:System"}, "cog", 2);
    }

    public static <T> T getService(Class<T> type) {
        if (type == ModuleContext.class) return (T) masterApp.moduleContext;

        return masterApp.moduleContext.getService(type);
    }
}
