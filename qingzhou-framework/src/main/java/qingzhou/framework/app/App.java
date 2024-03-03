package qingzhou.framework.app;

import qingzhou.api.*;

public interface App {
    String SYS_NODE_LOCAL = "local";

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

    QingZhouApp getQingZhouApp();

    AppContext getAppContext();

    void invoke(Request request, Response response) throws Exception;

    ModelBase getModelInstance(String modelName);
}