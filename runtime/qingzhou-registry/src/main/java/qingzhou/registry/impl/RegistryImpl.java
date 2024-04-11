package qingzhou.registry.impl;

import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.HashMap;
import java.util.Map;

public class RegistryImpl implements Registry {
    private final Map<String, InstanceInfo> instanceInfoMap = new HashMap<>();
    private final Map<String, AppInfo> appInfoMap = new HashMap<>();

    @Override
    public void register(InstanceInfo instanceInfo) {
        instanceInfoMap.put(instanceInfo.id, instanceInfo);
    }

    @Override
    public void register(AppInfo appInfo) {
        appInfoMap.put(appInfo.name, appInfo);
    }

    @Override
    public String[] getAllInstanceIds() {
        return instanceInfoMap.keySet().toArray(new String[0]);
    }

    @Override
    public InstanceInfo getInstanceInfo(String id) {
        return instanceInfoMap.get(id);
    }

    @Override
    public String[] getAllAppIds() {
        return appInfoMap.keySet().toArray(new String[0]);
    }

    @Override
    public AppInfo getAppInfo(String id) {
        return appInfoMap.get(id);
    }
}
