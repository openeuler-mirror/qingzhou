package qingzhou.console;

import qingzhou.api.metadata.AppMetadata;
import qingzhou.console.controller.SystemController;

import java.util.HashMap;
import java.util.Map;

public class AppMetadataManager {
    private static final AppMetadataManager instance = new AppMetadataManager();

    public static AppMetadataManager getInstance() {
        return instance;
    }

    private final Map<String, AppMetadata> appStubMap = new HashMap<>();

    public void registerAppStub(String appToken, AppMetadata appStub) {
        appStubMap.put(appToken, appStub);
    }

    // todo: unregisterAppStub 何时调用？
    public void unregisterAppStub(String appToken) {
        appStubMap.remove(appToken);
    }

    public AppMetadata getAppStub(String appName) {
        return appStubMap.computeIfAbsent(appName, s -> SystemController.getLocalApp(appName).getAppContext().getAppMetadata());
    }
}
