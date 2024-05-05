package qingzhou.deployer;

import qingzhou.api.QingzhouApp;
import qingzhou.engine.ModuleContext;

public abstract class QingzhouSystemApp implements QingzhouApp {
    protected ModuleContext moduleContext;
    protected Deployer deployer;

    public final void setDeployer(Deployer deployer) {
        this.deployer = deployer;
    }

    public final void setModuleContext(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }
}
