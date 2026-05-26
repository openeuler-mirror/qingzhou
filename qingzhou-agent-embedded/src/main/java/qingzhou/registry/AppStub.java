package qingzhou.registry;

import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.AppMeta;

public interface AppStub {
    AppMeta getAppMeta();
    void invokeApp(RequestImpl request) throws Throwable;
}