package qingzhou.framework;

import qingzhou.framework.api.Logger;

public interface FrameworkContext {
    String LOCAL_NODE_NAME = "local";
    String MASTER_APP_NAME = "master";
    String NODE_APP_NAME = "node";
    String NODEAGENT_MODEL_NAME = "nodeagent";
    String NODEAGENT_INSTALL_APP_ACTION_NAME = "install-app";
    String NODEAGENT_UN_INSTALL_APP_ACTION_NAME = "uninstall-app";

    boolean isMaster();

    Logger getLogger();

    AppStubManager getAppStubManager();

    AppManager getAppManager();

    ServiceManager getServiceManager();

    FileManager getFileManager();
}
