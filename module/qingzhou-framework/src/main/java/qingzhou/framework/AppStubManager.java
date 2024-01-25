package qingzhou.framework;

public interface AppStubManager {
    void registerAppStub(String appToken, AppStub appStub);

    AppStub getAppStub(String appToken);
}
