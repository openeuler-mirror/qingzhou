package qingzhou.app.node;

import org.osgi.framework.BundleContext;
import qingzhou.api.AppContext;
import qingzhou.app.QingZhouSystemApp;
import qingzhou.framework.Framework;

public class Main extends QingZhouSystemApp {
    private static Framework fc;
    private static BundleContext BC;

    @Override
    public void start(AppContext appContext) {
        fc = this.framework;
        BC = this.bundleContext;
    }

    public static Framework getFc() {
        return fc;
    }

    public static <T> T getService(Class<T> type) {
        return BC.getService(BC.getServiceReference(type));
    }
}
