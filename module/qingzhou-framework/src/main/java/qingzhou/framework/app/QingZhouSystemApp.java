package qingzhou.framework.app;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.QingZhouApp;

public abstract class QingZhouSystemApp extends QingZhouApp {
    protected FrameworkContext frameworkContext;

    public void setFrameworkContext(FrameworkContext frameworkContext) {
        this.frameworkContext = frameworkContext;
    }
}
