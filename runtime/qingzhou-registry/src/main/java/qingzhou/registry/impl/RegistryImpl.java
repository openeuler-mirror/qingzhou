package qingzhou.registry.impl;

import qingzhou.config.ConfigService;
import qingzhou.config.Remote;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyPairCipher;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.IPUtil;
import qingzhou.json.Json;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.*;

public class RegistryImpl implements Registry {
    private final Json json;
    private final CryptoService cryptoService;
    private final ConfigService configService;
    private final Map<String, InstanceInfo> instanceInfos = new HashMap<>();
    private final Map<String, String> instanceFingerprints = new HashMap<>();
    private InstanceInfo instanceInfo;

    public RegistryImpl(ModuleContext moduleContext) {
        this.json = moduleContext.getService(Json.class);
        this.cryptoService = moduleContext.getService(CryptoService.class);
        this.configService = moduleContext.getService(ConfigService.class);
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

    @Override
    public void register(InstanceInfo instanceInfo) {
        String fingerprint = this.cryptoService.getMessageDigest().fingerprint(this.json.toJson(instanceInfo));
        register(instanceInfo, instanceInfo.getId(), fingerprint);
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

    @Override
    public InstanceInfo thisInstanceInfo() throws Exception {
        if (instanceInfo == null) {
            instanceInfo = new InstanceInfo();
            instanceInfo.setId(UUID.randomUUID().toString().replace("-", ""));
            Remote remote = this.configService.getConfig().getRemote();
            instanceInfo.setName(remote.getName());
            instanceInfo.setHost(remote.getHost() != null && !remote.getHost().isEmpty() ? remote.getHost() : Arrays.toString(IPUtil.getLocalIps().toArray(new String[0])));
            instanceInfo.setPort(remote.getPort());
            KeyPairCipher keyPairCipher = this.cryptoService.getKeyPairCipher(remote.getMaster().getPublicKey(), null);
            String key = keyPairCipher.encryptWithPublicKey(UUID.randomUUID().toString());
            instanceInfo.setKey(key);
        }
        return instanceInfo;
    }
}
