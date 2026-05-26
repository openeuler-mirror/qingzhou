package qingzhou.registry;

import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;

import java.util.List;

public interface Registry {
    long getDataTimestamp();
    AppStub getAppStub(String instanceId, String appCode);
    InstanceInfo getLocalInstance();
    List<String> getAllLocalApps();
    AppStubLocal getLocalApp(String appCode);
}