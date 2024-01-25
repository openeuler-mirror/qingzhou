package qingzhou.framework;

public interface FrameworkContext {
    String SYS_NODE_LOCAL = "local";
    String SYS_APP_MASTER = "master";
    String SYS_APP_NODE_AGENT = "nodeagent";
    String NODE_AGENT_APP_INSTALLER_MODEL = "appinstaller";
    String NODE_AGENT_INSTALL_APP_ACTION = "install";
    String NODE_AGENT_UNINSTALL_APP_ACTION = "uninstall";

    boolean isMaster();

    AppStubManager getAppStubManager();

    AppManager getAppManager();

    ServiceManager getServiceManager();

    FileManager getFileManager();
}
