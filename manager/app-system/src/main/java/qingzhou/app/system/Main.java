package qingzhou.app.system;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;

@App
public class Main extends QingzhouSystemApp {
    private static Main main;

    @Override
    public void start(AppContext appContext) {
        main = this;

        appContext.addI18n("validator.exist", new String[]{"已存在", "en:Already exists"});
        appContext.addI18n("validator.require", new String[]{"不支持为空", "en:Cannot be empty"});
        appContext.addI18n("validator.master.system", new String[]{"为保障系统安全可用，请勿修改此配置", "en:To ensure the security and availability of the system, do not modify this configuration"});

        appContext.addMenu("Service", new String[]{"服务管理", "en:Service"}, "th-large", 1);
        appContext.addMenu("Monitor", new String[]{"监视管理", "en:Monitor"}, "server", 2);
        appContext.addMenu("System", new String[]{"系统管理", "en:System"}, "cog", 3);
    }

    public static <T> T getService(Class<T> type) {
        if (type == ModuleContext.class) return (T) main.moduleContext;

        return main.moduleContext.getService(type);
    }
}
