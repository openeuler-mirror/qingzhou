package qingzhou.registry.impl;

import qingzhou.engine.util.crypto.CryptoServiceFactory;
import qingzhou.json.Json;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryImpl implements Registry {
    private final Json json;
    private final Map<String, InstanceInfo> instanceInfos = new ConcurrentHashMap<>();
    private final Map<String, String> instanceFingerprints = new ConcurrentHashMap<>();
    private final Map<String, Long> instanceRegisterTimes = new ConcurrentHashMap<>();

    private static final Object LOCK = new Object();

    public RegistryImpl(Json json) {
        this.json = json;
    }

    @Override
    public boolean checkRegistered(String dataFingerprint) {
        synchronized (LOCK) {
            boolean registered = instanceFingerprints.containsKey(dataFingerprint);
            if (registered) {
                instanceRegisterTimes.put(dataFingerprint, System.currentTimeMillis());
            }
            return registered;
        }
    }

    @Override
    public void register(String registrationData) {
        InstanceInfo instanceInfo = json.fromJson(registrationData, InstanceInfo.class);
        register(instanceInfo, instanceInfo.getId(), CryptoServiceFactory.getInstance().getMessageDigest().fingerprint(registrationData));
    }

    private void register(InstanceInfo instanceInfo, String id, String fingerprint) {
        synchronized (LOCK) {
            // 查找旧指纹
            String oldFingerprint = null;
            for (Map.Entry<String, String> entry : instanceFingerprints.entrySet()) {
                if (entry.getValue().equals(id)) {
                    oldFingerprint = entry.getKey();
                    break;
                }
            }

            // 如果存在旧指纹，移除旧指纹相关的信息
            if (oldFingerprint != null) {
                instanceFingerprints.remove(oldFingerprint);
                instanceRegisterTimes.remove(oldFingerprint);
            }

            instanceInfos.put(id, instanceInfo);
            instanceFingerprints.put(fingerprint, id);
            instanceRegisterTimes.put(fingerprint, System.currentTimeMillis());
        }
    }

    public void clearTimeoutInstances(long timeout) {
        synchronized (LOCK) {
            long time = System.currentTimeMillis() - timeout;
            Iterator<Map.Entry<String, Long>> iterator = instanceRegisterTimes.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, Long> entry = iterator.next();
                if (entry.getValue() < time) {
                    String id = instanceFingerprints.remove(entry.getKey());
                    instanceInfos.remove(id);
                    iterator.remove(); // 使用迭代器的remove方法来安全删除元素
                }
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
