package qingzhou.framework;

import qingzhou.framework.api.Logger;

public interface FrameworkContext {
    String LOCAL_NODE_NAME = "local";
    String MASTER_APP_NAME = "master";
    String NODE_APP_NAME = "node";

    boolean isMaster();

    Logger getLogger();

    AppDeployer getAppDeployer();

    AppStubManager getAppStubManager();

    AppManager getAppManager();

    ServiceManager getServiceManager();

    FileManager getFileManager();
}
