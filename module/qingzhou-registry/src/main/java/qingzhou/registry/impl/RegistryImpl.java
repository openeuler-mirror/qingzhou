package qingzhou.registry.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.json.Json;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

class RegistryImpl implements Registry {
    private final Json json;
    private final CryptoService cryptoService;

    // 为支持数据分页，需要使用有序的Map实现
    private final Map<String, RegisteredInfo> registryInfo = new ConcurrentSkipListMap<>();

    RegistryImpl(Json json, CryptoService cryptoService) {
        this.json = json;
        this.cryptoService = cryptoService;
    }

    @Override
    public boolean checkRegistry(String dataFingerprint) {
        RegisteredInfo found = null;
        for (RegisteredInfo registeredInfo : registryInfo.values()) {
            if (registeredInfo.dataFingerprint.equals(dataFingerprint)) {
                found = registeredInfo;
                break;
            }
        }
        if (found == null) return false;

        found.registeredTimeMillis = System.currentTimeMillis();
        return true;
    }

    @Override
    public void register(String registrationData) {
        InstanceInfo instanceInfo = json.fromJson(registrationData, InstanceInfo.class);
        String fingerprint = cryptoService.getMessageDigest().fingerprint(registrationData);

        registryInfo.put(instanceInfo.getName(),
                new RegisteredInfo(
                        instanceInfo,
                        System.currentTimeMillis(),
                        fingerprint));
    }

    @Override
    public List<String> getAllInstanceNames() {
        return new ArrayList<>(registryInfo.keySet());
    }

    @Override
    public InstanceInfo getInstanceInfo(String instanceName) {
        RegisteredInfo registeredInfo = registryInfo.get(instanceName);
        if (registeredInfo == null) return null;
        return registeredInfo.instanceInfo;
    }

    @Override
    public List<String> getAllAppNames() {
        List<String> appNames = new ArrayList<>();
        registryInfo.values().forEach(reg -> Arrays.stream(reg.instanceInfo.getAppInfos()).forEach(ap -> appNames.add(ap.getName())));
        appNames.sort(String::compareTo);
        return appNames;
    }

    @Override
    public AppInfo getAppInfo(String appName) {
        for (RegisteredInfo registeredInfo : registryInfo.values()) {
            for (AppInfo appInfo : registeredInfo.instanceInfo.getAppInfos()) {
                if (appInfo.getName().equals(appName)) {
                    return appInfo;
                }
            }
        }

        return null;
    }

    @Override
    public List<String> getAppInstanceNames(String appName) {
        List<String> found = new ArrayList<>();
        for (RegisteredInfo registeredInfo : registryInfo.values()) {
            for (AppInfo appInfo : registeredInfo.instanceInfo.getAppInfos()) {
                if (appInfo.getName().equals(appName)) {
                    found.add(registeredInfo.instanceInfo.getName());
                    break;
                }
            }
        }
        return found;
    }

    // 周期执行，可进行过期清理等操作
    void timerCheck() {
        long minLegalTime = System.currentTimeMillis() - 1000 * 30;

        List<String> toDelete = new ArrayList<>();
        for (Map.Entry<String, RegisteredInfo> entry : registryInfo.entrySet()) {
            if (entry.getValue().registeredTimeMillis < minLegalTime) {
                toDelete.add(entry.getKey());
            }
        }

        toDelete.forEach(registryInfo::remove);
    }

    static class RegisteredInfo {
        final InstanceInfo instanceInfo;
        final String dataFingerprint;
        long registeredTimeMillis;

        RegisteredInfo(InstanceInfo instanceInfo, long registeredTimeMillis, String dataFingerprint) {
            this.instanceInfo = instanceInfo;
            this.registeredTimeMillis = registeredTimeMillis;
            this.dataFingerprint = dataFingerprint;
        }
    }
}
