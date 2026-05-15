package qingzhou.registry.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.*;
import qingzhou.api.Constants;
import qingzhou.crypto.Crypto;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStub;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.AppStubRemote;
import qingzhou.registry.Registry;

@Component
public class RegistryImpl implements Registry {
    private final List<String> tempMsg = new ArrayList<>();

    @Reference
    private Logger logger;
    @Reference
    private Json json;
    @Reference
    private HttpClient httpClient;
    @Reference
    private Crypto crypto;

    private String qzVersion;
    private InstanceInfo localInstanceInfo;
    private long dataTimestamp = Long.MAX_VALUE;

    private final Map<String, AppStubLocal> localApps = java.util.Collections
            .synchronizedMap(new java.util.LinkedHashMap<>());

    private final Map<String, InstanceInfo> remoteApps = java.util.Collections
            .synchronizedMap(new java.util.LinkedHashMap<>());

    @Activate
    public synchronized void start() {
        qzVersion = System.getProperty("qingzhou.version"); // 缓存，防止系统参数被应用覆盖
        qzVersion = new File(qzVersion).getName().substring("version".length());

        tempMsg.forEach(s -> logger.info(s));
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindApp(AppStubLocal appStub) {
        bindApp0(appStub);
        dataTimestamp = System.currentTimeMillis();
    }

    private synchronized void bindApp0(AppStubLocal appStub) {
        AppMeta appMeta = appStub.getAppMeta();
        String appCode = appMeta.getApp().code;
        localApps.put(appCode, appStub);

        String msg = String.format("app registered: %s", appCode);
        if (logger != null) { // osgi ds 尚未规范：AppStubLocal 的注入 可能早于 logger
            logger.info(msg);
        } else {
            tempMsg.add(msg);
        }
    }

    /**
     * OSGI 调用规则：如果没有指定 @Reference.unbind 方法名，那默认最终要求是 “ un + bind 方法名字”
     * The name of the unbind method is created from the name of the @Reference
     * annotated method.
     * If the name of the annotated method begins with bind, set or add,
     * that prefix is replaced with unbind, unset or remove,
     * respectively, to create the name candidate for the unbind method.
     * Otherwise, un is prefixed to the name of the annotated method to create the
     * name candidate for the unbind method.
     */
    public void unbindApp(AppStubLocal appStub) {
        unbindApp0(appStub);
        dataTimestamp = System.currentTimeMillis();
    }

    private void unbindApp0(AppStubLocal appStub) {
        AppMeta appMeta = appStub.getAppMeta();
        String appCode = appMeta.getApp().code;
        localApps.remove(appCode);

        logger.info(String.format("app unregistered: %s", appCode));
    }

    @Override
    public long getDataTimestamp() {
        return dataTimestamp;
    }

    @Override
    public AppStub getAppStub(String instanceId, String appCode) {
        if (instanceId.equals(Constants.LOCAL_INSTANCE_ID)) {
            return getLocalApp(appCode);
        } else {
            return getRemoteApp(instanceId, appCode);
        }
    }

    @Override
    public InstanceInfo getLocalInstance() {
        if (localInstanceInfo == null) {
            localInstanceInfo = new InstanceInfo() {
                @Override
                public List<AppMeta> getAppMetas() {
                    return localApps.values().stream().map(AppStub::getAppMeta).collect(Collectors.toList());
                }

                {
                    this.setId(Constants.LOCAL_INSTANCE_ID);
                    this.setVersion(qzVersion);
                    this.setHost(Utils.getLocalIp());
                }
            };
        }
        return localInstanceInfo;
    }

    @Override
    public List<String> getAllLocalApps() {
        return new ArrayList<>(localApps.keySet());
    }

    @Override
    public AppStubLocal getLocalApp(String appCode) {
        return localApps.get(appCode);
    }

    @Override
    public List<String> getAllRemoteInstances() {
        return new ArrayList<>(remoteApps.keySet());
    }

    @Override
    public InstanceInfo getRemoteInstance(String instanceId) {
        return remoteApps.get(instanceId);
    }

    @Override
    public List<String> getAllRemoteApps(String instanceId) {
        InstanceInfo instanceInfo = remoteApps.get(instanceId);
        if (instanceInfo == null) return null;
        return instanceInfo.getAppMetas().stream().map(appMeta -> appMeta.getApp().code).collect(Collectors.toList());
    }

    @Override
    public AppStubRemote getRemoteApp(String instanceId, String appCode) {
        InstanceInfo instanceInfo = remoteApps.get(instanceId);
        if (instanceInfo == null) return null;
        for (AppMeta appMeta : instanceInfo.getAppMetas()) {
            if (appMeta.getApp().code.equals(appCode)) {
                return new AppStubRemoteImpl(instanceInfo, appMeta, json, httpClient, crypto);
            }
        }
        return null;
    }

    public InstanceInfo addRemoteApps(InstanceInfo instanceInfo) {
        InstanceInfo exists = remoteApps.put(instanceInfo.getId(), instanceInfo);
        if (exists == null) {
            dataTimestamp = System.currentTimeMillis();
        }
        return exists;
    }

    public void removeRemoteApps(String instanceId) {
        remoteApps.remove(instanceId);
        dataTimestamp = System.currentTimeMillis();
    }
}
