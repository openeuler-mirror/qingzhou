package qingzhou.app.instance;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;

@App
public class InstanceApp extends QingzhouSystemApp {
    private static ModuleContext MODULECONTEXT;

    @Override
    public void start(AppContext appContext) {
        MODULECONTEXT = this.moduleContext;
    }

    public static <T> T getService(Class<T> type) {
        return MODULECONTEXT.getService(type);
    }
}
