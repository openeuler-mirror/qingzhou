package qingzhou.framework.impl;

import qingzhou.framework.AppManager;
import qingzhou.framework.ConfigManager;
import qingzhou.framework.FileManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.ServiceManager;

public class FrameworkContextImpl implements FrameworkContext {
    private static final String qzVerName = "version";

    private final ServiceManager serviceManager = new ServiceManagerImpl();
    private final FileManager fileManager = new FileManagerImpl();

    @Override
    public String getName() {
        return "QingZhou（轻舟）";
    }

    @Override
    public String getVersion() {
        return fileManager.getLib().getName().substring(qzVerName.length());
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
    public FileManager getFileManager() {
        return fileManager;
    }

    @Override
    public ConfigManager getConfigManager() {
        return serviceManager.getService(ConfigManager.class);
    }
}
