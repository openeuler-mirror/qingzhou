package qingzhou.app.master;

import qingzhou.api.AppContext;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;

import java.io.File;

@qingzhou.api.App
public class MasterApp extends QingzhouSystemApp {
    private static MasterApp masterApp;

    @Override
    public void start(AppContext appContext) {
        masterApp = this;

        appContext.addI18n("validator.exist", new String[]{"已存在", "en:Already exists"});
        appContext.addI18n("validator.require", new String[]{"不支持为空", "en:Cannot be empty"});
        appContext.addI18n("validator.master.system", new String[]{"为保障系统安全可用，请勿修改此配置", "en:To ensure the security and availability of the system, do not modify this configuration"});
        appContext.addMenu("Service", new String[]{"服务管理", "en:Service"}, "th-large", 1);
        appContext.addMenu("System", new String[]{"系统管理", "en:System"}, "cog", 2);
    }

    public static File getInstanceDir() {
        return masterApp.moduleContext.getInstanceDir();
    }

    public static File getLibDir() {
        return masterApp.moduleContext.getLibDir();
    }

    public static <T> T getService(Class<T> type) {
        if (type == ModuleContext.class) return (T) masterApp.moduleContext;

        return masterApp.moduleContext.getService(type);
    }
}
