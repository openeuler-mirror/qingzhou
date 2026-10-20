package qingzhou.agent;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.*;
import qingzhou.common.Configs;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.EpochPacketCipher;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.Registry;

@Component(configurationPid = "qingzhou-agent", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Heartbeat {
    @Reference
    private ConfigurationAdmin configAdmin;

    @Reference
    private HttpClient httpClient;
    @Reference
    private Crypto crypto;
    @Reference
    private Registry registry;
    @Reference
    private Json json;
    @Reference
    private Logger logger;

    private String domainId;
    private String registerUrl;
    private Timer timer;
    private String qzVersion;

    private static volatile InstanceInfo thisInstanceInfo;

    // 本域唯一根密钥：发送盖章与解密 registry 下行报文共用
    private static volatile byte[] rootKey;

    // 上次构建的各 AppMeta 序列化指纹：心跳比对复用，避免每拍全量重复序列化
    private volatile List<String> storedFingerprints;

    static byte[] rootKey() {
        return rootKey;
    }

    static InstanceInfo currentInstanceInfo() {
        return thisInstanceInfo;
    }

    @Activate
    public void start(Map<String, String> config) {
        Configs configs = new Configs(config);
        qzVersion = configs.get("qingzhou.version"); // 缓存，防止系统参数被应用覆盖
        AgentDomainKeys domainKeys = new AgentDomainKeys(configs.get("qingzhou.instance"));
        domainId = domainKeys.domain();
        rootKey = domainKeys.master();

        String registryUrl = configs.notBlank("url");
        while (registryUrl.endsWith("/")) {
            registryUrl = registryUrl.substring(0, registryUrl.length() - 1);
        }
        registerUrl = registryUrl + "/registry/register";

        timer = new Timer("agent-heartbeat");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (appChanged()) {
                        buildInstanceInfo();
                    }
                    // 心跳即注册：注册报文同时承担存活保活（registry 健康检查依据 lastRefreshTime）。
                    // 报文体为实例信息 JSON；时间戳与 nonce 由 ReplayGuard 封装进报文头，AAD 绑定 domainId
                    String registration = send(registerUrl, json.toJson(thisInstanceInfo).getBytes(StandardCharsets.UTF_8));
                    if (registration != null && !"Redundant.".equals(registration)) {
                        logger.info("instance registered: " + registration);
                    }
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

    private String send(String url, byte[] data) {
        Response response = null;
        try {
            byte[] domain = domainId.getBytes(StandardCharsets.UTF_8);
            EpochPacketCipher packetCipher = crypto.getEpochPacketCipher(rootKey, domainId);
            // 报文头自带时间戳与 nonce（时间窗 + 去重），AAD 绑定 domainId 防报文挪域
            byte[] encrypted = packetCipher.encrypt(data, domain);
            // 报文头：域名明文首行供 registry 查表（域名同时是 AAD，篡改即认证失败）
            byte[] body = new byte[domain.length + 1 + encrypted.length];
            System.arraycopy(domain, 0, body, 0, domain.length);
            body[domain.length] = '\n';
            System.arraycopy(encrypted, 0, body, domain.length + 1, encrypted.length);
            // 显式 POST：请求必须带密文正文，且 registry 侧已限定 POST
            response = httpClient.send(
                    httpClient.buildRequest(url).method(HttpMethod.POST).body(body));
            if (response.getStatus() != 200) {
                logger.warn("response code error [ " + response.getStatus() + " ] : " + url);
                return null; // 非 200 不当作注册成功，避免上层打印空的“已注册”日志
            }
            byte[] responseBody = response.getBody();

            if (responseBody == null || responseBody.length == 0) return "";

            // 响应同样是时段报文：按报文自带时间戳派生密钥解开
            byte[] result = packetCipher.decrypt(responseBody, domain);
            return new String(result, StandardCharsets.UTF_8);
        } catch (ConnectException e) {
            logger.error("failed to connect to the URL: " + url + ", msg: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            // 释放底层连接资源，避免每次心跳累积泄漏
            if (response != null) response.cancel();
        }
        return null;
    }

    private void buildInstanceInfo() throws Exception {
        InstanceInfo instanceInfo = new InstanceInfo();

        // 实例部署的应用（已按 appCode 排序，保证注册报文与实例 ID 的 MD5 计算稳定）
        List<AppMeta> appMetas = currentAppMetas();
        instanceInfo.getAppMetas().addAll(appMetas);

        // 实例版本
        instanceInfo.setPlatformVersion(qzVersion);

        // 实例端口
        Dictionary<String, Object> httpServerConfig = configAdmin.getConfiguration("qingzhou-http-server", null).getProperties();
        instanceInfo.setPort(Integer.parseInt(String.valueOf(httpServerConfig.get("port"))));
        instanceInfo.setSslEnabled(Boolean.parseBoolean(String.valueOf(httpServerConfig.get("ssl_enabled"))));

        // 所有信息收集完成后，计算实例 ID，同一个实例多次重启的 ID 不应该变化
        String digestTemp = json.toJson(instanceInfo);
        digestTemp += ips(); // 集群下的多实例，除了IP别的都是一样的，加入 IP 标识可区分集群中的不同实例
        String uniqueDataId = crypto.getMessageDigest().md5(digestTemp);
        instanceInfo.setId(uniqueDataId);

        storedFingerprints = fingerprints(appMetas); // 与 appChanged 同源构建，保证比对口径一致
        thisInstanceInfo = instanceInfo;
    }

    private String ips() throws Exception {
        List<String> ips = new ArrayList<>();
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        while (nets.hasMoreElements()) {
            NetworkInterface ni = nets.nextElement();
            if (ni.isLoopback() || !ni.isUp()) continue;
            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress()) continue;
                ips.add(inetAddress.getHostAddress());
            }
        }
        Collections.sort(ips); // 确保生成的指纹不依赖于遍历顺序，保证稳定性。

        StringBuilder allIps = new StringBuilder();
        ips.forEach(allIps::append);
        return allIps.toString();
    }
}
