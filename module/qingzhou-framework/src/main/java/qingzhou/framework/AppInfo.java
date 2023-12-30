package qingzhou.framework;

import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;

public interface AppInfo {
    AppContext getAppContext();

    void invokeAction(Request request, Response response) throws Exception;
}
