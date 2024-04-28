package qingzhou.app.instance;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;

@App
public class InstanceApp extends QingzhouSystemApp {
    private static ModuleContext MODULECONTEXT;
    private static Deployer DEPLOYER;

    @Override
    public void start(AppContext appContext) {
        MODULECONTEXT = this.moduleContext;
        DEPLOYER = this.deployer;
    }

    public static <T> T getService(Class<T> type) {
        return (T) (type == Deployer.class ? DEPLOYER : MODULECONTEXT);
    }
}
