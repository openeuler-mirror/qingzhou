package qingzhou.console;

import qingzhou.console.controller.SystemController;

import java.util.HashMap;
import java.util.Map;

public class AppMetadataManager {
    private static final AppMetadataManager instance = new AppMetadataManager();

    public static AppMetadataManager getInstance() {
        return instance;
    }

    private final Map<String, AppMetadata> appMetadataMap = new HashMap<>();

    public void registerApp(String appName, AppMetadata appMetadata) {
        appMetadataMap.put(appName, appMetadata);
    }

    // todo: 何时调用？
    public void unregisterApp(String appName) {
        appMetadataMap.remove(appName);
    }

    public AppMetadata getAppMetadata(String appName) {
        return appMetadataMap.computeIfAbsent(appName, s -> SystemController.getLocalApp(appName).getAppContext().getAppMetadata());
    }
}
