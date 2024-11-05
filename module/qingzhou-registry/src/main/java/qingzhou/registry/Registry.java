package qingzhou.registry;

import qingzhou.engine.ServiceInfo;

import java.util.List;

public interface Registry extends ServiceInfo {
    @Override
    default boolean isAppShared() {
        return false;
    }

    boolean checkRegistry(String dataFingerprint);

    void register(String registrationData); // 远程注册，数据为 json 格式

    List<String> getAllInstanceNames();

    InstanceInfo getInstanceInfo(String instanceName);

    List<String> getAllAppNames();

    AppInfo getAppInfo(String appName);

    List<String> getAppInstanceNames(String appName);
}
