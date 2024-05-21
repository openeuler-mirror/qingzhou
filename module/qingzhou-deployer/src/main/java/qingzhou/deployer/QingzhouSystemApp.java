package qingzhou.deployer;

import qingzhou.api.QingzhouApp;
import qingzhou.engine.ModuleContext;
import qingzhou.registry.Registry;

public abstract class QingzhouSystemApp implements QingzhouApp {
    protected ModuleContext moduleContext;
    protected Deployer deployer;
    protected Registry registry;

    public final void setDeployer(Deployer deployer) {
        this.deployer = deployer;
    }

    public final void setModuleContext(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    public final void setRegistry(Registry registry) {
        this.registry = registry;
    }
}
