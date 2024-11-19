package qingzhou.core.registry;

import qingzhou.engine.Service;

import java.util.List;

@Service(shareable = false)
public interface Registry {
    boolean checkRegistry(String dataFingerprint);

    void register(String registrationData); // 远程注册，数据为 json 格式

    List<String> getAllInstanceNames();

    InstanceInfo getInstanceInfo(String instanceName);

    List<String> getAllAppNames();

    AppInfo getAppInfo(String appName);

    List<String> getAppInstanceNames(String appName);
}
