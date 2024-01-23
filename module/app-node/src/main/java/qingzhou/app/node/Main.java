package qingzhou.app.node;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.QingZhouSystemApp;
import qingzhou.framework.api.AppContext;

public class Main extends QingZhouSystemApp {
    private static FrameworkContext fc;

    @Override
    public void start(AppContext appContext) {
        fc = this.frameworkContext;
    }

    public static FrameworkContext getFc() {
        return fc;
    }
}
