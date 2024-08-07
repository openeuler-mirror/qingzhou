package qingzhou.app.instance;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;

import java.io.File;

@App
public class InstanceApp extends QingzhouSystemApp {
    private static ModuleContext MODULECONTEXT;

    @Override
    public void start(AppContext appContext) {
        MODULECONTEXT = this.moduleContext;
        appContext.addI18n("validator.exist", new String[]{"已存在", "en:Already exists"});

        appContext.addMenu("Monitor", new String[]{"监视", "en:Monitor"}, "server", 1);
    }

    public static File getInstanceDir() {
        return MODULECONTEXT.getInstanceDir();
    }

    public static <T> T getService(Class<T> type) {
        if (ModuleContext.class == type) {
            return (T) MODULECONTEXT;
        }
        return MODULECONTEXT.getService(type);
    }
}
