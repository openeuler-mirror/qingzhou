package qingzhou.core;

import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.api.Request;

public interface App {
    QingzhouApp getQingzhouApp();

    AppContext getAppContext();

    AppInfo getAppInfo();

    void invoke(Request request) throws Exception;
}