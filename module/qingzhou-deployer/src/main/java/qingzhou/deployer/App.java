package qingzhou.deployer;

import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.registry.AppInfo;

public interface App {
    QingzhouApp getQingzhouApp();

    AppContext getAppContext();

    AppInfo getAppInfo();

    void invoke(Request request, Response response) throws Exception;

    void invokeDirectly(Request request, Response response) throws Exception;

    void invokeDefault(Request request, Response response) throws Exception;
}