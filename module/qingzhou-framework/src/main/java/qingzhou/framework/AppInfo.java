package qingzhou.framework;

import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.QingZhouApp;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;

import java.util.Properties;

public interface AppInfo {
    QingZhouApp getQingZhouApp();

    AppContext getAppContext();

    Properties getAppProperties();

    void invokeAction(Request request, Response response) throws Exception;
}
