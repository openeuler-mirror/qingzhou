package qingzhou.registry.impl;

import qingzhou.engine.util.crypto.CryptoServiceFactory;
import qingzhou.json.Json;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RegistryImpl implements Registry {
    private final Json json;
    private final Map<String, InstanceInfo> instanceInfos = new HashMap<>();
    private final Map<String, String> instanceFingerprints = new HashMap<>();
    private final Map<String, Long> instanceRegisterTimes = new HashMap<>();

    public RegistryImpl(Json json) {
        this.json = json;
    }

    @Override
    public boolean checkRegistered(String dataFingerprint) {
        boolean registered = instanceFingerprints.containsKey(dataFingerprint);
        if (registered) {
            instanceRegisterTimes.put(dataFingerprint, System.currentTimeMillis());
        }

        return registered;
    }

    @Override
    public void register(String registrationData) {
        InstanceInfo instanceInfo = json.fromJson(registrationData, InstanceInfo.class);
        register(instanceInfo, instanceInfo.getId(), CryptoServiceFactory.getInstance().getMessageDigest().fingerprint(registrationData));
    }

    private void register(InstanceInfo instanceInfo, String id, String fingerprint) {
        instanceInfos.put(id, instanceInfo);
        instanceFingerprints.put(fingerprint, id);
        instanceRegisterTimes.put(fingerprint, System.currentTimeMillis());
    }

    public void clearTimeoutInstances(long timeout) {
        long time = System.currentTimeMillis() - timeout;
        Iterator<Map.Entry<String, Long>> iterator = instanceRegisterTimes.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() < time) {
                instanceInfos.remove(instanceFingerprints.remove(entry.getKey()));
                iterator.remove(); // 使用迭代器的remove方法来安全删除元素
            }
        }

    }

    @Override
    public Collection<String> getAllInstanceId() {
        return instanceInfos.keySet();
    }

    @Override
    public InstanceInfo getInstanceInfo(String id) {
        return instanceInfos.get(id);
    }

    @Override
    public AppInfo getAppInfo(String appName) {
        for (InstanceInfo instanceInfo : instanceInfos.values()) {
            for (AppInfo appInfo : instanceInfo.getAppInfos()) {
                if (appInfo.getName().equals(appName)) {
                    return appInfo;
                }
            }
        }
        throw new IllegalArgumentException("App not found: " + appName);
    }
}
