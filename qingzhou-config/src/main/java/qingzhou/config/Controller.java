package qingzhou.config;

import qingzhou.bootstrap.main.ServiceRegister;
import qingzhou.framework.config.Config;
import qingzhou.framework.crypto.CryptoService;

public class Controller extends ServiceRegister<Config> {
    @Override
    public Class<Config> serviceType() {
        return Config.class;
    }

    @Override
    protected Config serviceObject() {
        return new LocalConfig(
                this.frameworkContext,
                this.frameworkContext.getService(CryptoService.class)
        );
    }
}
