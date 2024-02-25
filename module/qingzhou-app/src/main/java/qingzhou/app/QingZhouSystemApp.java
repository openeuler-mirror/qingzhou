package qingzhou.app;

import org.osgi.framework.BundleContext;
import qingzhou.api.QingZhouApp;
import qingzhou.framework.Framework;

public abstract class QingZhouSystemApp extends QingZhouApp {
    protected Framework framework;
    protected BundleContext bundleContext;

    public void setFrameworkContext(Framework framework) {
        this.framework = framework;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
