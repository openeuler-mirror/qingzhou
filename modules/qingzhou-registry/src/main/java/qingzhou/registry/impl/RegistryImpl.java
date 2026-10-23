package qingzhou.registry.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.*;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.logger.util.PendingLogs;
import qingzhou.registry.*;
import qingzhou.registry.web.Invoke;

@Component(configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RegistryImpl implements Registry {
    @Reference
    private ConfigurationAdmin configAdmin;
    @Reference
    Logger logger;
    @Reference
    Json json;
    @Reference
    HttpClient httpClient;
    @Reference
    AgentDomainKeys agentDomainKeys;

    private final PendingLogs pendingLogs = new PendingLogs();

    Path uploadBase;
    private String qzVersion;
    private InstanceInfo localInstanceInfo;
    // 单调递增的数据版本：用计数器而非毫秒墙钟，避免同一毫秒内的多次变更互相覆盖
    private final AtomicLong registryDataVersion = new AtomicLong(System.currentTimeMillis());

    private final Map<String, AppStubLocal> localApps = Collections.synchronizedMap(new LinkedHashMap<>());

    private final Map<String, InstanceInfo> remoteInstances = Collections.synchronizedMap(new LinkedHashMap<>());

    @Activate
    public void start(Map<String, String> config) {
        qzVersion = config.get("qingzhou.version"); // 缓存，防止系统参数被应用覆盖
        uploadBase = Paths.get(config.get("qingzhou.instance"), "temp", Invoke.UPLOAD_TEMP_NAME);

        pendingLogs.replay(logger);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindApp(AppStubLocal appStub) {
        bindApp0(appStub);
        registryDataVersion.incrementAndGet();
    }

    private void bindApp0(AppStubLocal appStub) {
        AppMeta appMeta = appStub.getAppMeta();
        String appCode = appMeta.getApp().code;
        localApps.put(appCode, appStub);

        pendingLogs.info(logger, String.format("app registered: %s", appCode));
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
        registryDataVersion.incrementAndGet();
    }

    private void unbindApp0(AppStubLocal appStub) {
        AppMeta appMeta = appStub.getAppMeta();
        String appCode = appMeta.getApp().code;
        // 两参 remove：动态重绑时同 code 的新服务可能已覆盖，仅当仍是本服务时才移除
        localApps.remove(appCode, appStub);

        logger.info(String.format("app unregistered: %s", appCode));
    }

    @Override
    public long getRegistryDataVersion() {
        return registryDataVersion.get();
    }

    @Override
    public AppStub getAppStub(String instanceId, String appCode) {
        if (instanceId == null) {
            return null;
        }
        if (instanceId.equals(qingzhou.registry.Constants.LOCAL_INSTANCE_ID)) {
            return getLocalApp(appCode);
        } else {
            return getRemoteApp(instanceId, appCode);
        }
    }

    @Override
    public synchronized InstanceInfo getLocalInstance() {
        if (localInstanceInfo == null) {
            localInstanceInfo = new InstanceInfo() {
                @Override
                public List<AppMeta> getAppMetas() {
                    return localApps.values().stream().map(AppStub::getAppMeta).collect(Collectors.toList());
                }
            };
            localInstanceInfo.setId(Constants.LOCAL_INSTANCE_ID);
            localInstanceInfo.setPlatformVersion(qzVersion);
            localInstanceInfo.setHost("localhost");

            // 实例端口
            try {
                Dictionary<String, Object> httpServerConfig = configAdmin.getConfiguration("qingzhou-http-server", null).getProperties();
                if (httpServerConfig == null) { // 配置缺失不阻断实例列表：端口留空由调用方按未就绪处理
                    logger.warn("missing configuration: qingzhou-http-server");
                } else {
                    localInstanceInfo.setPort(Integer.parseInt(String.valueOf(httpServerConfig.get("port"))));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
        return new ArrayList<>(remoteInstances.keySet());
    }

    @Override
    public InstanceInfo getRemoteInstance(String instanceId) {
        return remoteInstances.get(instanceId);
    }

    @Override
    public List<String> getAllRemoteApps(String instanceId) {
        InstanceInfo instanceInfo = remoteInstances.get(instanceId);
        if (instanceInfo == null) return null;
        return instanceInfo.getAppMetas().stream().map(appMeta -> appMeta.getApp().code).collect(Collectors.toList());
    }

    @Override
    public AppStubRemote getRemoteApp(String instanceId, String appCode) {
        InstanceInfo instanceInfo = remoteInstances.get(instanceId);
        if (instanceInfo == null) return null;
        for (AppMeta appMeta : instanceInfo.getAppMetas()) {
            if (appMeta.getApp().code.equals(appCode)) {
                return new AppStubRemoteImpl(this, instanceInfo, appMeta);
            }
        }
        return null;
    }

    @Override
    public void addRemoteInstance(InstanceInfo instanceInfo) {
        // 先填充存活时间再发布：健康检查线程可能在此期间遍历表，
        // 若先 put 后填，新对象会短暂带着 lastRefreshTime=0 出现，被立即剔除（实例周期性抖动的根因之一）。
        // 重复注册同样要填：新对象的 lastRefreshTime 为 0，不填则重注册恢复路径必然被健康检查删除。
        instanceInfo.refreshTime();
        remoteInstances.put(instanceInfo.getId(), instanceInfo);
        registryDataVersion.incrementAndGet();
    }

    @Override
    public void removeRemoteInstance(String instanceId) {
        remoteInstances.remove(instanceId);
        registryDataVersion.incrementAndGet();
    }
}
