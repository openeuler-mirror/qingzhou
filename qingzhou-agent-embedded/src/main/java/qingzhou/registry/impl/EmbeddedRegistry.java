package qingzhou.registry.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import qingzhou.crypto.Crypto;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.*;

public class EmbeddedRegistry implements Registry {
    private final Logger logger;
    private final Json json;
    private final Crypto crypto;
    private long dataTimestamp;

    private final Map<String, AppStubLocal> localApps = new ConcurrentHashMap<>();
    private InstanceInfo localInstance;

    public EmbeddedRegistry(Logger logger, Json json, Crypto crypto) {
        this.logger = logger;
        this.json = json;
        this.crypto = crypto;
    }

    @Override
    public long getDataTimestamp() {
        return dataTimestamp;
    }

    @Override
    public AppStub getAppStub(String instanceId, String appCode) {
        if (localInstance != null && localInstance.getId().equals(instanceId)) {
            return getLocalApp(appCode);
        }
        return null;
    }

    @Override
    public InstanceInfo getLocalInstance() {
        return localInstance;
    }

    public void setLocalInstance(InstanceInfo instanceInfo) {
        this.localInstance = instanceInfo;
    }

    @Override
    public List<String> getAllLocalApps() {
        return new ArrayList<>(localApps.keySet());
    }

    @Override
    public AppStubLocal getLocalApp(String appCode) {
        return localApps.get(appCode);
    }

    public void registerLocalApp(String appCode, AppStubLocal appStub) {
        localApps.put(appCode, appStub);
        dataTimestamp = System.currentTimeMillis();
        logger.info("App registered: " + appCode);
    }

    public void unregisterLocalApp(String appCode) {
        localApps.remove(appCode);
        dataTimestamp = System.currentTimeMillis();
        logger.info("App unregistered: " + appCode);
    }
}