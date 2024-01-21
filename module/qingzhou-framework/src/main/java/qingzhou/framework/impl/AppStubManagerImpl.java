package qingzhou.framework.impl;

import qingzhou.framework.AppStubManager;
import qingzhou.framework.api.AppStub;

import java.util.HashMap;
import java.util.Map;

public class AppStubManagerImpl implements AppStubManager {
    private final Map<String, AppStub> appStubMap = new HashMap<>();

    @Override
    public void registerAppStub(String appToken, AppStub appStub) {
        appStubMap.put(appToken, appStub);
    }

    @Override
    public AppStub getAppStub(String appToken) {
        return appStubMap.get(appToken);
    }
}
