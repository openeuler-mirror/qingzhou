package qingzhou.core.deployer;

import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.api.Request;
import qingzhou.core.registry.AppInfo;

public interface App {
    QingzhouApp getQingzhouApp();

    AppContext getAppContext();

    AppInfo getAppInfo();

    void invoke(Request request) throws Exception;
}