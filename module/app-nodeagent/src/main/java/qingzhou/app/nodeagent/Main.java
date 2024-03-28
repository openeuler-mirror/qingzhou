package qingzhou.app.nodeagent;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.framework.app.QingzhouSystemApp;

@App
public class Main extends QingzhouSystemApp {
    private static FrameworkContext fc;

    @Override
    public void start(AppContext appContext) {
        fc = this.frameworkContext;
    }

    public static FrameworkContext getFc() {
        return fc;
    }

    public static <T> T getService(Class<T> type) {
        return fc.getServiceManager().getService(type);
    }
}
