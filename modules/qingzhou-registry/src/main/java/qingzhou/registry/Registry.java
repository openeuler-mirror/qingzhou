package qingzhou.registry;

import java.util.List;

import qingzhou.dto.meta.InstanceInfo;

public interface Registry {
    long getRegistryDataVersion(); // 可用于前端缓存元数据

    AppStub getAppStub(String instanceId, String appCode);

    AppStubLocal getLocalApp(String appCode);

    List<String> getAllLocalApps();

    AppStubRemote getRemoteApp(String instanceId, String appCode);

    List<String> getAllRemoteApps(String instanceId);

    InstanceInfo getLocalInstance();

    InstanceInfo getRemoteInstance(String instanceId);

    List<String> getAllRemoteInstances();

    void addRemoteInstance(InstanceInfo instanceInfo);

    void removeRemoteInstance(String instanceId);
}
