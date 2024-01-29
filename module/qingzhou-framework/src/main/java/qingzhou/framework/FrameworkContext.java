package qingzhou.framework;

public interface FrameworkContext {
    String SYS_NODE_LOCAL = "local";
    String SYS_APP_MASTER = "master";
    String SYS_APP_NODE_AGENT = "nodeagent";
    String SYS_APP_COMMON = "common";
    String SYS_ACTION_MANAGE = "manage";
    String NODE_AGENT_INSTALL_APP_MODEL = "appinstaller";
    String NODE_AGENT_INSTALL_APP_ACTION = "install";
    String NODE_AGENT_UNINSTALL_APP_ACTION = "uninstall";

    AppManager getAppManager();

    ServiceManager getServiceManager();

    FileManager getFileManager();

    ConfigManager getConfigManager();
}
