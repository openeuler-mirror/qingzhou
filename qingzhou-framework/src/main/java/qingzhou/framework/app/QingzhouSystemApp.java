package qingzhou.framework.app;

import qingzhou.api.QingzhouApp;
import qingzhou.bootstrap.main.FrameworkContext;

public abstract class QingzhouSystemApp extends QingzhouApp {
    protected FrameworkContext frameworkContext;

    public void setModuleContext(FrameworkContext frameworkContext) {
        this.frameworkContext = frameworkContext;
    }
}
