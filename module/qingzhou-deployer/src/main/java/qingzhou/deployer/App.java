package qingzhou.deployer;

import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.api.Request;
import qingzhou.registry.AppInfo;

public interface App {
    QingzhouApp getQingzhouApp();

    AppContext getAppContext();

    AppInfo getAppInfo();

    void invoke(Request request) throws Exception;
}