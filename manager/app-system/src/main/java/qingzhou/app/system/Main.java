package qingzhou.app.system;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;

@App
public class Main extends QingzhouSystemApp {
    public static final String SERVICE_MENU = "Service";
    public static final String SETTING_MENU = "Setting";
    private static Main main;

    @Override
    public void start(AppContext appContext) {
        main = this;

        appContext.addI18n("validator.exist", new String[]{"已存在", "en:Already exists"});
        appContext.addI18n("validator.require", new String[]{"不支持为空", "en:Cannot be empty"});

        appContext.addMenu(Main.SERVICE_MENU, new String[]{"服务管理", "en:Service"}, "th-large", 1);
        appContext.addMenu(Main.SETTING_MENU, new String[]{"系统设置", "en:System"}, "cog", 3);
    }

    public static <T> T getService(Class<T> type) {
        if (type == ModuleContext.class) return (T) main.moduleContext;

        return main.moduleContext.getService(type);
    }
}
