package qingzhou.deployer;

import qingzhou.api.QingzhouApp;
import qingzhou.crypto.CryptoService;
import qingzhou.engine.ModuleContext;

public abstract class QingzhouSystemApp extends QingzhouApp {
    protected ModuleContext moduleContext;
    protected Deployer deployer;
    protected CryptoService cryptoService;

    public final void setDeployer(Deployer deployer) {
        this.deployer = deployer;
    }

    public final void setModuleContext(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    public final void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }
}
