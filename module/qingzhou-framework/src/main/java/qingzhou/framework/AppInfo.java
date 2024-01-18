package qingzhou.framework;

import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;

import java.util.Properties;

public interface AppInfo {
    Properties getAppProperties();

    AppContext getAppContext();

    void invokeAction(Request request, Response response) throws Exception;
}
