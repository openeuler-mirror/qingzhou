package qingzhou.registry;

import java.util.List;

import qingzhou.dto.meta.InstanceInfo;

public interface Registry {
    long getRegistryDataVersion(); // 可用于前端缓存元数据

    AppStub getAppStub(String instanceId, String appCode);

    InstanceInfo getLocalInstance();

    List<String> getAllLocalApps();

    AppStubLocal getLocalApp(String appCode);

    List<String> getAllRemoteInstances();

    InstanceInfo getRemoteInstance(String instanceId);

    List<String> getAllRemoteApps(String instanceId);

    AppStubRemote getRemoteApp(String instanceId, String appCode);
}
