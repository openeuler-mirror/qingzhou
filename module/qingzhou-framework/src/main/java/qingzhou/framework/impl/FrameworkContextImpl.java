package qingzhou.framework.impl;

import qingzhou.framework.*;

public class FrameworkContextImpl implements FrameworkContext {
    private final ServiceManager serviceManager = new ServiceManagerImpl();
    private final FileManager fileManager = new FileManagerImpl();

    @Override
    public AppManager getAppManager() {
        return serviceManager.getService(AppManager.class);
    }

    @Override
    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    @Override
    public FileManager getFileManager() {
        return fileManager;
    }

    @Override
    public ConfigManager getConfigManager() {
        return serviceManager.getService(ConfigManager.class);
    }
}
