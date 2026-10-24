package qingzhou.agent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.*;
import qingzhou.common.Configs;
import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainKeys;
import qingzhou.registry.Constants;
import qingzhou.registry.Registry;

@Component(configurationPid = "qingzhou-agent", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Heartbeat {
    @Reference
    private ConfigurationAdmin configAdmin;

    @Reference
    private HttpClient httpClient;
    @Reference
    private Registry registry;
    @Reference
    private Json json;
    @Reference
    private Logger logger;
    @Reference
    private AgentDomainKeys agentDomainKeys;
    @Reference
    private LocalAgentDomainKey localAgentDomainKey;

    private String qzVersion;
    private int serverPort;
    private boolean serverSslEnabled;
    private String registerUrl;
    // 反代/NAT 部署下本实例的对外可达地址：未配置则由 registry 按连接源地址推断
    private String advertisedHost;
    private Timer timer;
    private List<String> storedFingerprints;
    // 是否已被中心登记（当前 instanceId + secret 已上报且中心返回 200/304）
    private boolean registered;
    // 已注册后的最小心跳载荷：中心对已存在实例仅续期(304)、不解析 body，故无需携带 InstanceInfo/instanceSecret
    private static final byte[] HEARTBEAT_BODY = "{}".getBytes(StandardCharsets.UTF_8);

    private static volatile InstanceInfo thisInstanceInfo;

    static InstanceInfo currentInstanceInfo() {
        return thisInstanceInfo;
    }

    @Activate
    public void start(Map<String, String> config) throws IOException {
        Configs configs = new Configs(config);
        qzVersion = configs.get("qingzhou.version"); // 缓存，防止系统参数被应用覆盖

        Dictionary<String, Object> httpServerConfig = configAdmin.getConfiguration("qingzhou-http-server", null).getProperties();
        if (httpServerConfig == null) { // 端口是注册与回连的必需项：缺失须快速失败并指明配置项
            throw new IllegalStateException("missing configuration: qingzhou-http-server");
        }
        serverPort = Integer.parseInt(String.valueOf(httpServerConfig.get("port")));
        serverSslEnabled = Boolean.parseBoolean(String.valueOf(httpServerConfig.get("ssl_enabled")));

        String registryUrl = configs.notBlank("url");
        while (registryUrl.endsWith("/")) {
            registryUrl = registryUrl.substring(0, registryUrl.length() - 1);
        }
        registerUrl = registryUrl + "/registry" + qingzhou.registry.Constants.REMOTE_REGISTER_URI;
        advertisedHost = configs.get("advertised_host");

        timer = new Timer("agent-heartbeat");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (appChanged()) buildInstanceInfo();

                    send(); // 心跳即注册：注册报文同时承担存活保活（registry 健康检查依据 lastRefreshTime）。
                } catch (Exception e) {
                    logger.error("heartbeat service error", e);
                }
            }
        }, 0L, 1000L * configs.atLeast("interval", 2, 6));
    }

    @Deactivate
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private boolean appChanged() throws Exception {
        List<String> stored = storedFingerprints;
        if (stored == null) return true;

        // 按内容比较而非引用：AppMeta 的默认值等字段可能被就地修改而引用不变
        return !fingerprints(currentAppMetas()).equals(stored);
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

    private void buildInstanceInfo() throws Exception {
        InstanceInfo instanceInfo = new InstanceInfo();
        // 重启 ID 变化，依赖 Registry 的过期清理
        instanceInfo.setId(UUID.randomUUID().toString());
        // 每实例独立密钥：与实例同生命周期，仅存内存并随首次注册上报；用于派生每实例密钥
        // （center→agent 报文加密 + 心跳续期认证），仅真实实例可凭它刷新自身
        instanceInfo.setInstanceSecret(UUID.randomUUID().toString());
        instanceInfo.setAdvertisedHost(advertisedHost);
        // 实例版本
        instanceInfo.setPlatformVersion(qzVersion);
        // 实例端口
        instanceInfo.setPort(serverPort);
        instanceInfo.setSslEnabled(serverSslEnabled);
        // 实例部署的应用（已按 appCode 排序，保证注册报文与实例 ID 的 MD5 计算稳定）
        List<AppMeta> appMetas = currentAppMetas();
        instanceInfo.getAppMetas().addAll(appMetas);

        thisInstanceInfo = instanceInfo;
        registered = false; // 新 id + 新 secret，需重新完整注册

        storedFingerprints = fingerprints(appMetas); // 与 appChanged 同源构建，保证比对口径一致
    }

    private void send() {
        try {
            InstanceInfo instanceInfo = currentInstanceInfo();
            // 已登记后：续期报文改用每实例密钥加密（仅真实实例可刷新自身），且不再序列化 InstanceInfo
            // （含 instanceSecret）——既缩小 secret 暴露窗口，又防止同域其他 agent 冒名续期
            byte[] data = registered ? HEARTBEAT_BODY : json.toJson(instanceInfo).getBytes(StandardCharsets.UTF_8);
            EpochKeyRotatingCipher cipher = registered
                    ? localAgentDomainKey.getInstanceCipher(instanceInfo.getInstanceSecret())
                    : localAgentDomainKey.getLocalAgentCipher();
            byte[] aad = agentDomainKeys.toRegistryAad(localAgentDomainKey.getLocalAgentDomain(), instanceInfo.getId(), qingzhou.registry.Constants.REMOTE_REGISTER_URI);
            byte[] encrypted = cipher.encrypt(data, aad);
            String url = registerUrl + "?"
                    + qingzhou.registry.Constants.REMOTE_PARAMETER_DOMAIN_ID + "=" + encode(localAgentDomainKey.getLocalAgentDomain())
                    + "&" + Constants.REMOTE_PARAMETER_INSTANCE_ID + "=" + encode(instanceInfo.getId());
            Response response = httpClient.send(httpClient.buildRequest(url).method(HttpMethod.POST).body(encrypted));

            if (response.getStatus() == 304) {
                registered = true;
                return;
            }

            if (response.getStatus() == 200) {
                if (registered) { // 发的最小载荷却被当新实例登记（中心重启）：登记信息不完整，需重发完整信息
                    logger.warn("unexpected 200 on minimal heartbeat, will resend full registration");
                    registered = false;
                } else {
                    logger.info(instanceInfo.getAppMetas().size() + " apps registered");
                    registered = true;
                }
            } else {
                logger.error("response code error [ " + response.getStatus() + " ] : " + registerUrl);
                registered = false;
            }
        } catch (ConnectException e) {
            logger.error("failed to connect to the URL: " + registerUrl + ", msg: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8"); // Java 8 无 Charset 重载
    }
}
