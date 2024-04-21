package qingzhou.registry.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.json.Json;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RegistryImpl implements Registry {
    private final Json json;
    private final CryptoService cryptoService;
    private final Map<String, InstanceInfo> instanceInfos = new HashMap<>();
    private final Map<String, String> instanceFingerprints = new HashMap<>();

    public RegistryImpl(Json json, CryptoService cryptoService) {
        this.json = json;
        this.cryptoService = cryptoService;
    }

    @Override
    public boolean checkRegistered(String dataFingerprint) {
        return instanceFingerprints.containsKey(dataFingerprint);
    }

    @Override
    public void register(String registrationData) {
        InstanceInfo instanceInfo = json.fromJson(registrationData, InstanceInfo.class);
        register(instanceInfo, instanceInfo.getId(), this.cryptoService.getMessageDigest().fingerprint(registrationData));
    }

    private void register(InstanceInfo instanceInfo, String id, String fingerprint) {
        instanceInfos.put(id, instanceInfo);
        instanceFingerprints.put(fingerprint, id);
    }

    @Override
    public Collection<String> getAllInstanceId() {
        return instanceInfos.keySet();
    }

    @Override
    public InstanceInfo getInstanceInfo(String id) {
        return instanceInfos.get(id);
    }
}
