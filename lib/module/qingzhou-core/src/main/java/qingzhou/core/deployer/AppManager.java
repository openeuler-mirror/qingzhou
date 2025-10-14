package qingzhou.core.deployer;

import qingzhou.api.AppContext;
import qingzhou.api.Request;
import qingzhou.core.registry.AppInfo;

public interface AppManager {
    AppContext getAppContext();

    AppInfo getAppInfo();

    void invoke(Request request) throws Throwable;
}