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
import qingzhou.dto.Constants;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainKeys;
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
    private Timer timer;
    private List<String> storedFingerprints;

    private static volatile InstanceInfo thisInstanceInfo;

    static InstanceInfo currentInstanceInfo() {
        return thisInstanceInfo;
    }

    @Activate
    public void start(Map<String, String> config) throws IOException {
        Configs configs = new Configs(config);
        qzVersion = configs.get("qingzhou.version"); // 缓存，防止系统参数被应用覆盖

        Dictionary<String, Object> httpServerConfig = configAdmin.getConfiguration("qingzhou-http-server", null).getProperties();
        serverPort = Integer.parseInt(String.valueOf(httpServerConfig.get("port")));
        serverSslEnabled = Boolean.parseBoolean(String.valueOf(httpServerConfig.get("ssl_enabled")));

        String registryUrl = configs.notBlank("url");
        while (registryUrl.endsWith("/")) {
            registryUrl = registryUrl.substring(0, registryUrl.length() - 1);
        }
        registerUrl = registryUrl + "/registry" + Constants.REMOTE_REGISTER_URI;

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
        // 实例版本
        instanceInfo.setPlatformVersion(qzVersion);
        // 实例端口
        instanceInfo.setPort(serverPort);
        instanceInfo.setSslEnabled(serverSslEnabled);
        // 实例部署的应用（已按 appCode 排序，保证注册报文与实例 ID 的 MD5 计算稳定）
        List<AppMeta> appMetas = currentAppMetas();
        instanceInfo.getAppMetas().addAll(appMetas);

        thisInstanceInfo = instanceInfo;

        storedFingerprints = fingerprints(appMetas); // 与 appChanged 同源构建，保证比对口径一致
    }

    private void send() {
        try {
            EpochKeyRotatingCipher cipher = localAgentDomainKey.getLocalAgentCipher();
            InstanceInfo instanceInfo = currentInstanceInfo();
            byte[] data = json.toJson(instanceInfo).getBytes(StandardCharsets.UTF_8);
            byte[] aad = agentDomainKeys.toRegistryAad(instanceInfo.getId(), Constants.REMOTE_REGISTER_URI);
            byte[] encrypted = cipher.encrypt(data, aad);
            String url = registerUrl + "?"
                    + Constants.REMOTE_PARAMETER_DOMAIN_ID + "=" + encode(localAgentDomainKey.getLocalAgentDomain())
                    + "&" + Constants.REMOTE_PARAMETER_INSTANCE_ID + "=" + encode(instanceInfo.getId());
            Response response = httpClient.send(httpClient.buildRequest(url).method(HttpMethod.POST).body(encrypted));

            if (response.getStatus() == 304) return;

            if (response.getStatus() == 200) {
                logger.info(instanceInfo.getAppMetas().size() + " apps registered");
            } else {
                logger.error("response code error [ " + response.getStatus() + " ] : " + registerUrl);
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
