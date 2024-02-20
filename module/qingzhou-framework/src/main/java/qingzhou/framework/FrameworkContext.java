package qingzhou.framework;

import qingzhou.framework.app.AppManager;
import qingzhou.framework.service.ServiceManager;

public interface FrameworkContext {
    String SYS_NODE_LOCAL = "local";

    String MANAGE_TYPE_APP = "app";
    String MANAGE_TYPE_NODE = "node";

    String SYS_APP_MASTER = "master";
    String SYS_APP_NODE_AGENT = "nodeagent";

    String SYS_MODEL_INDEX = "index";
    String SYS_MODEL_HOME = "home";
    String SYS_MODEL_APP_INSTALLER = "appinstaller";
    String SYS_MODEL_APP = "app";
    String SYS_MODEL_NODE = "node";

    String SYS_ACTION_MANAGE = "manage";
    String SYS_ACTION_INSTALL = "install";
    String SYS_ACTION_UNINSTALL = "uninstall";

    // 轻舟产品名词
    String getName();

    // 产品版本信息
    String getVersion();

    AppManager getAppManager();

    ServiceManager getServiceManager();

    ConfigManager getConfigManager();
}
