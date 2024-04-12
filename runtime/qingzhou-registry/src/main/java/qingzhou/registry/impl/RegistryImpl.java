package qingzhou.registry.impl;

import qingzhou.json.Json;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RegistryImpl implements Registry {
    private final Json json;
    private final Map<String, InstanceInfo> instanceInfos = new HashMap<>();

    public RegistryImpl(Json json) {
        this.json = json;
    }

    @Override
    public void register(String registrationData) {
        InstanceInfo instanceInfo = json.fromJson(registrationData, InstanceInfo.class);
        instanceInfos.put(instanceInfo.id, instanceInfo);
    }

    @Override
    public Collection<String> getAllInstanceId() {
        return instanceInfos.keySet();
    }

    @Override
    public InstanceInfo getInstanceInfo(String id) {
        InstanceInfo instanceInfo = instanceInfos.get(id);
        return json.fromJson(json.toJson(instanceInfo), InstanceInfo.class);// 复制一份，防止元数据被篡改
    }
}
