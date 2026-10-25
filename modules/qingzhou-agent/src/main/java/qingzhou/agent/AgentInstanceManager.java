package qingzhou.agent;

import java.io.IOException;
import java.util.*;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.json.Json;
import qingzhou.registry.Registry;

@Component(configurationPid = "qingzhou-agent", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = AgentInstanceManager.class)
public class AgentInstanceManager {
    @Reference
    private ConfigurationAdmin configAdmin;
    @Reference
    private Registry registry;
    @Reference
    private Json json;

    private String qzVersion;
    // 反代/NAT 部署下本实例的对外可达地址：未配置则由 registry 按连接源地址推断
    private String advertisedHost;
    private int serverPort;
    private boolean serverSslEnabled;

    private volatile CachedInstanceInfo cachedInstanceInfo;

    @Activate
    public void start(Map<String, String> config) throws IOException {
        qzVersion = config.get("qingzhou.version"); // 缓存，防止系统参数被应用覆盖
        advertisedHost = config.get("advertised_host");

        Dictionary<String, Object> httpServerConfig = configAdmin.getConfiguration("qingzhou-http-server", null).getProperties();
        if (httpServerConfig == null) { // 端口是注册与回连的必需项：缺失须快速失败并指明配置项
            throw new IllegalStateException("missing configuration: qingzhou-http-server");
        }
        serverPort = Integer.parseInt(String.valueOf(httpServerConfig.get("port")));
        serverSslEnabled = Boolean.parseBoolean(String.valueOf(httpServerConfig.get("ssl_enabled")));
    }

    InstanceInfo getCachedInstance() {
        if (cachedInstanceInfo == null) return null;
        return cachedInstanceInfo.instanceInfo;
    }

    void reCacheInstanceInfo() throws Exception {
        InstanceInfo instanceInfo = new InstanceInfo();
        // 每实例独立密钥：与实例同生命周期，仅存内存并随首次注册上报；用于派生每实例密钥
        // （registry→agent 报文加密 + 心跳续期认证），仅真实实例可凭它刷新自身
        instanceInfo.setInstanceSecret(UUID.randomUUID().toString());
        instanceInfo.setHost(advertisedHost);
        // 实例版本
        instanceInfo.setPlatformVersion(qzVersion);
        // 实例端口
        instanceInfo.setPort(serverPort);
        instanceInfo.setSslEnabled(serverSslEnabled);
        // 实例部署的应用（已按 appCode 排序，保证注册报文与实例 ID 的 MD5 计算稳定）
        List<AppMeta> appMetas = currentAppMetas();
        instanceInfo.getAppMetas().addAll(appMetas);

        cachedInstanceInfo = new CachedInstanceInfo(instanceInfo, fingerprints(appMetas));
    }

    boolean appChanged() throws Exception {
        if (cachedInstanceInfo == null) return true;

        // 按内容比较而非引用：AppMeta 的默认值等字段可能被就地修改而引用不变
        return !fingerprints(currentAppMetas()).equals(cachedInstanceInfo.fingerprints);
    }

    // 以 appCode 排序收集本地应用：注册报文、实例 ID 与指纹的稳定性由此保证
    private List<AppMeta> currentAppMetas() {
        List<AppMeta> appMetas = new ArrayList<>();
        for (String appCode : registry.getAllLocalApps()) {
            appMetas.add(registry.getLocalApp(appCode).getAppMeta());
        }
        appMetas.sort(Comparator.comparing(appMeta -> appMeta.getApp().code));
        return appMetas;
    }

    private List<String> fingerprints(List<AppMeta> appMetas) throws Exception {
        List<String> fingerprints = new ArrayList<>(appMetas.size());
        for (AppMeta appMeta : appMetas) {
            fingerprints.add(json.toJson(appMeta));
        }
        return fingerprints;
    }

    private static class CachedInstanceInfo {
        final InstanceInfo instanceInfo;
        final List<String> fingerprints;

        CachedInstanceInfo(InstanceInfo instanceInfo, List<String> fingerprints) {
            this.instanceInfo = instanceInfo;
            this.fingerprints = fingerprints;
        }
    }
}
