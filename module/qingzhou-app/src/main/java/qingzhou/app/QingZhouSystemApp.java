package qingzhou.app;

import qingzhou.api.QingZhouApp;
import qingzhou.framework.Framework;

public abstract class QingZhouSystemApp extends QingZhouApp {
    protected Framework framework;

    public void setFrameworkContext(Framework framework) {
        this.framework = framework;
    }
}
