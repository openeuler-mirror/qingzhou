package qingzhou.framework.impl;

import qingzhou.framework.AppManager;
import qingzhou.framework.ConfigManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.ServiceManager;

public class FrameworkContextImpl implements FrameworkContext {
    private final ServiceManager serviceManager = new ServiceManagerImpl();

    @Override
    public String getName() {
        return "Qingzhou（轻舟）";
    }

    @Override
    public String getVersion() {
        return getConfigManager().getVersion();
    }

    @Override
    public AppManager getAppManager() {
        return serviceManager.getService(AppManager.class);
    }

    @Override
    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    @Override
    public ConfigManager getConfigManager() {
        return serviceManager.getService(ConfigManager.class);
    }
}
