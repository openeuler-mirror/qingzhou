package qingzhou.framework;

import qingzhou.framework.api.AppStub;

public interface AppStubManager {
    void registerAppStub(String appToken, AppStub appStub);

    AppStub getAppStub(String appToken);
}
