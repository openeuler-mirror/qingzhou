package qingzhou.framework.impl;

import qingzhou.framework.AppManager;
import qingzhou.framework.AppStubManager;
import qingzhou.framework.FileManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.ServiceManager;
import qingzhou.framework.api.Logger;
import qingzhou.framework.util.FileUtil;

import java.io.File;

public class FrameworkContextImpl implements FrameworkContext {
    private final AppStubManagerImpl appStubManager = new AppStubManagerImpl();
    private final ServiceManagerImpl serviceManager = new ServiceManagerImpl();
    private final FileManagerImpl fileManager = new FileManagerImpl();
    private Boolean isMaster;

    @Override
    public AppStubManager getAppStubManager() {
        return appStubManager;
    }

    @Override
    public boolean isMaster() {
        if (isMaster == null) {
            File console = FileUtil.newFile(getFileManager().getLib(), "sysapp", "console");
            isMaster = console.isDirectory();
        }

        return isMaster;
    }

    @Override
    public Logger getLogger() {
        return serviceManager.getService(Logger.class);
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
}
