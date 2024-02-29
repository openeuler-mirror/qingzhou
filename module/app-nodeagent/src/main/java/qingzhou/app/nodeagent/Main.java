package qingzhou.app.nodeagent;

import qingzhou.api.AppContext;
import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.framework.app.QingZhouSystemApp;

public class Main extends QingZhouSystemApp {
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
