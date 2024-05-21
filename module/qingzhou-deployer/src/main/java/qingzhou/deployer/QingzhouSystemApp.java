package qingzhou.deployer;

import qingzhou.api.QingzhouApp;
import qingzhou.engine.ModuleContext;

public abstract class QingzhouSystemApp implements QingzhouApp {
    protected ModuleContext moduleContext;

    public final void setModuleContext(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }
}
