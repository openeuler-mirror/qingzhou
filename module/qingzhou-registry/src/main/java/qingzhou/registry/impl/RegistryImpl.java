package qingzhou.registry.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.json.Json;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryImpl implements Registry {
    private final Json json;
    private final CryptoService cryptoService;
    private final Map<String, RegisteredInfo> registryInfo = new ConcurrentHashMap<>();

    public RegistryImpl(Json json, CryptoService cryptoService) {
        this.json = json;
        this.cryptoService = cryptoService;
    }

    @Override
    public synchronized boolean checkRegistered(String dataFingerprint) {
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
        registryInfo.put(instanceInfo.getId(),
                new RegisteredInfo(
                        instanceInfo,
                        System.currentTimeMillis(),
                        fingerprint));
    }

    public synchronized void clearTimeoutInstances(long timeout) {
        long minLegalTime = System.currentTimeMillis() - timeout;

        List<String> toDelete = new ArrayList<>();
        for (Map.Entry<String, RegisteredInfo> entry : registryInfo.entrySet()) {
            if (entry.getValue().registeredTimeMillis < minLegalTime) {
                toDelete.add(entry.getKey());
            }
        }

        toDelete.forEach(registryInfo::remove);
    }

    @Override
    public Collection<String> getAllInstanceId() {
        return registryInfo.keySet();
    }

    @Override
    public InstanceInfo getInstanceInfo(String id) {
        return registryInfo.get(id).instanceInfo;
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

    private static class RegisteredInfo {
        final InstanceInfo instanceInfo;
        final String dataFingerprint;
        long registeredTimeMillis;

        private RegisteredInfo(InstanceInfo instanceInfo, long registeredTimeMillis, String dataFingerprint) {
            this.instanceInfo = instanceInfo;
            this.registeredTimeMillis = registeredTimeMillis;
            this.dataFingerprint = dataFingerprint;
        }
    }
}
