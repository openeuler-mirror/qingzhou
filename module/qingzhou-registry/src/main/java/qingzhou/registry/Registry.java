package qingzhou.registry;

import java.util.Collection;

public interface Registry {
    boolean checkRegistry(String dataFingerprint);

    void register(String registrationData); // 远程注册，数据为 json 格式

    Collection<String> getAllInstanceId();

    InstanceInfo getInstanceInfo(String id);

    AppInfo getAppInfo(String appName);
}
