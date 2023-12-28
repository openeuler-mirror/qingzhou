package qingzhou.framework;

import qingzhou.api.AppContext;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;

public interface AppInfo {
    AppContext getAppContext();

    void invokeAction(Request request, Response response) throws Exception;
}
