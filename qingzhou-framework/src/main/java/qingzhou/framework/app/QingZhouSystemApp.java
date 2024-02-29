package qingzhou.framework.app;

import qingzhou.api.QingZhouApp;
import qingzhou.bootstrap.main.FrameworkContext;

public abstract class QingZhouSystemApp extends QingZhouApp {
    protected FrameworkContext frameworkContext;

    public void setModuleContext(FrameworkContext frameworkContext) {
        this.frameworkContext = frameworkContext;
    }
}
