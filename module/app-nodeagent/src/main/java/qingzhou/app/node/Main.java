package qingzhou.app.node;

import qingzhou.api.AppContext;
import qingzhou.app.QingZhouSystemApp;
import qingzhou.framework.Framework;

public class Main extends QingZhouSystemApp {
    private static Framework fc;

    @Override
    public void start(AppContext appContext) {
        fc = this.framework;
    }

    public static Framework getFc() {
        return fc;
    }
}
