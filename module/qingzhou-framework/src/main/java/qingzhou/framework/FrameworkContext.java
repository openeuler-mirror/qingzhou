package qingzhou.framework;

public interface FrameworkContext {
    String SYS_NODE_LOCAL = "local";

    String MANAGE_TYPE_APP = "app";
    String MANAGE_TYPE_NODE = "node";

    String SYS_APP_MASTER = "master";
    String SYS_APP_NODE_AGENT = "nodeagent";
    String SYS_APP_COMMON = "common";

    String SYS_MODEL_INDEX = "index";
    String SYS_MODEL_Home = "home";
    String SYS_MODEL_APP_INSTALLER = "appinstaller";

    String SYS_ACTION_MANAGE = "manage";
    String SYS_ACTION_INSTALL = "install";
    String SYS_ACTION_UNINSTALL = "uninstall";

    AppManager getAppManager();

    ServiceManager getServiceManager();

    FileManager getFileManager();

    ConfigManager getConfigManager();
}
