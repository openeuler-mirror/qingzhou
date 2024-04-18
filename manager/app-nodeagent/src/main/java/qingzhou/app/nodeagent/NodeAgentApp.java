package qingzhou.app.nodeagent;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;

@App
public class NodeAgentApp extends QingzhouSystemApp {
    private static ModuleContext fc;

    @Override
    public void start(AppContext appContext) {
        fc = this.moduleContext;
    }

    public static ModuleContext getFc() {
        return fc;
    }

    public static <T> T getService(Class<T> type) {
        return fc.getService(type);
    }
}
