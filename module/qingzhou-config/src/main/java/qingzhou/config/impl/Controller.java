package qingzhou.config.impl;

import qingzhou.framework.ConfigManager;
import qingzhou.framework.service.ServiceRegister;

public class Controller extends ServiceRegister<ConfigManager> {
    @Override
    protected Class<ConfigManager> serviceType() {
        return ConfigManager.class;
    }

    @Override
    protected ConfigManager serviceObject() {
        return new LocalConfigManager(frameworkContext);
    }
}
