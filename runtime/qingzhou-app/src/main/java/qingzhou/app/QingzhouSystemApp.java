package qingzhou.app;

import qingzhou.api.QingzhouApp;
import qingzhou.engine.ModuleContext;

public abstract class QingzhouSystemApp extends QingzhouApp {
    protected ModuleContext moduleContext;

    public void setModuleContext(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }
}
