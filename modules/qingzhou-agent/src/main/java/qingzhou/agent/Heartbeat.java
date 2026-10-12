package qingzhou.agent;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.*;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.PairCipher;
import qingzhou.dto.Constants;
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

    private PairCipher pairCipher;
    private String registerToken;
    private String refreshUrl;
    private String registerUrl;
    private Timer timer;
    private String qzVersion;

    static volatile InstanceInfo thisInstanceInfo;


    // 轮换前的旧钥：跨线程读取（Netty 线程处理 agent 请求，Timer 线程轮换密钥）
    private static volatile String previousKey;
    private static volatile long previousKeyAt;

    /**
     * 宽限期内返回轮换前的旧钥，供 ProcessHandler 解密回退；过期或无轮换返回 null
     */
    static String decryptKeyFallback() {
        String key = previousKey;
        if (key == null) return null;

        if (System.currentTimeMillis() - previousKeyAt <= Constants.AGENT_KEY_GRACE_MILLIS * 2) {
            return key;
        } else {
            previousKey = null;
            return null;
        }
    }

    @Activate
    public void start(Map<String, String> config) throws Exception {
        qzVersion = config.get("qingzhou.version"); // 缓存，防止系统参数被应用覆盖

        pairCipher = crypto.getPairCipher(config.get("public_key"), null);

        String registerTokenConfig = config.get("register_token");
        registerToken = registerTokenConfig == null || registerTokenConfig.trim().isEmpty() ? null : registerTokenConfig.trim();

        String registryUrl = config.get("url");
        if (registryUrl == null || registryUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("qingzhou-agent.url can not be empty");
        }
        while (registryUrl.endsWith("/")) {
            registryUrl = registryUrl.substring(0, registryUrl.length() - 1);
        }
        refreshUrl = registryUrl + "/registry/refresh";
        registerUrl = registryUrl + "/registry/register";

        timer = new Timer("agent-heartbeat");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (appChanged()) {
                        buildInstanceInfo();
                    }

                    String newKey = crypto.generateKey(); // 更新共享的对称密钥，以保障前向安全！
                    // 带上当前密钥作为持有证明：registry 公钥不保密，仅凭它不足以授权改密钥
                    String refreshInfos = thisInstanceInfo.getId() + "," + newKey + "," + thisInstanceInfo.getKey();
                    String refreshed = send(refreshUrl, refreshInfos.getBytes(StandardCharsets.UTF_8));
                    if (Boolean.parseBoolean(refreshed)) { // 服务端已经刷新了密钥
                        // 宽限切换：registry 已切新钥，但其 invoke 在宽限期内仍用旧钥加密（本响应也是旧钥密文），
                        // 保留旧钥一段时间令窗口内的报文仍可解密，invoke 不再因轮换必然失败
                        previousKey = thisInstanceInfo.getKey();
                        previousKeyAt = System.currentTimeMillis();

                        thisInstanceInfo.setKey(newKey);
                    } else {
                        // 注册报文：registerToken + timestamp + nonce + currentKeyProof + instanceInfoJson。
                        // timestamp 与 nonce 供 registry 做时间窗与重放判定；currentKeyProof = 用当前实例密钥
                        // 加密 nonce，重复注册必须证明持有当前密钥，否则重放的旧注册包可回滚共享密钥。
                        // 若密钥已失配（证明被拒），registry 会在健康检查超时后移除实例，随后本注册即可通过
                        String nonce = UUID.randomUUID().toString();
                        String currentKeyProof = "";
                        try {
                            Cipher cipher = crypto.getCipher(thisInstanceInfo.getKey());
                            currentKeyProof = cipher.encrypt(nonce);
                        } catch (Exception e) {
                            logger.error("failed to build registration proof", e);
                        }
                        String registerData = String.join(Constants.AGENT_REGISTER_TOKEN_SP,
                                registerToken == null ? "" : registerToken,
                                String.valueOf(System.currentTimeMillis()),
                                nonce,
                                currentKeyProof,
                                json.toJson(thisInstanceInfo));
                        String registration = send(registerUrl, registerData.getBytes(StandardCharsets.UTF_8));
                        logger.info("registration response: " + registration);
                    }
                } catch (Exception e) {
                    logger.error("heartbeat service error", e);
                }
            }
        }, 1000 * 10, 1000 * Long.parseLong(config.getOrDefault("interval", "6")));
    }

    @Deactivate
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private boolean appChanged() {
        InstanceInfo instanceInfo = thisInstanceInfo;
        if (instanceInfo == null) return true;

        List<AppMeta> appMetaList = instanceInfo.getAppMetas();
        List<AppMeta> currentAppMetas = currentAppMetas();
        if (appMetaList.size() != currentAppMetas.size()) return true;

        Comparator<AppMeta> comparator = Comparator.comparing(o -> o.getApp().code);
        appMetaList.sort(comparator); // 固定顺序，避免指纹计算错误
        currentAppMetas.sort(comparator); // 固定顺序，避免指纹计算错误
        for (int i = 0; i < appMetaList.size(); i++) {
            if (appMetaList.get(i) != currentAppMetas.get(i)) return true;
        }

        return false;
    }

    private String send(String url, byte[] data) {
        try {
            byte[] encrypted = pairCipher.encryptWithPublicKey(data);
            // 显式 POST：请求必须带密文正文，且 registry 侧已限定 POST
            Response response = httpClient.send(
                    httpClient.buildRequest(url).method(HttpMethod.POST).body(encrypted));
            if (response.getStatus() != 200) {
                logger.warn("response code error [ " + response.getStatus() + " ] : " + url);
            }
            byte[] responseBody = response.getBody();

            if (responseBody == null || responseBody.length == 0) return "";

            Cipher cipher = crypto.getCipher(thisInstanceInfo.getKey());
            byte[] result = cipher.decrypt(responseBody);
            return new String(result, StandardCharsets.UTF_8);
        } catch (ConnectException e) {
            logger.error("failed to connect to the URL: " + url + ", msg: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private List<AppMeta> currentAppMetas() {
        List<AppMeta> appMetas = new ArrayList<>();
        for (String appCode : registry.getAllLocalApps()) {
            AppMeta appMeta = registry.getLocalApp(appCode).getAppMeta();
            appMetas.add(appMeta);
        }
        return appMetas;
    }

    private void buildInstanceInfo() throws Exception {
        InstanceInfo thisInstanceInfo = new InstanceInfo();

        // 实例部署的应用
        List<AppMeta> currentAppMetas = currentAppMetas();
        currentAppMetas.sort(Comparator.comparing(appMeta -> appMeta.getApp().code)); // 固定顺序，避免指纹计算错误
        thisInstanceInfo.getAppMetas().addAll(currentAppMetas);

        // 实例版本
        thisInstanceInfo.setVersion(qzVersion);

        // 实例端口
        Dictionary<String, Object> httpServerConfig = configAdmin.getConfiguration("qingzhou-http-server", null).getProperties();
        int serverPort = Integer.parseInt(String.valueOf(httpServerConfig.get("port")));
        thisInstanceInfo.setPort(serverPort);

        boolean sslEnabled = Boolean.parseBoolean(String.valueOf(httpServerConfig.get("ssl_enabled")));
        thisInstanceInfo.setSslEnabled(sslEnabled);

        // 所有信息收集完成后，计算实例 ID，同一个实例多次重启的 ID 不应该变化
        String digestTemp = json.toJson(thisInstanceInfo);
        digestTemp += ips(); // 集群下的多实例，除了IP别的都是一样的，加入 IP 标识可区分集群中的不同实例
        String uniqueDataId = crypto.getMessageDigest().md5(digestTemp);
        thisInstanceInfo.setId(uniqueDataId);

        // 实例密钥，动态生成的，放在 ID 计算之前，不参与 ID 计算
        thisInstanceInfo.setKey(crypto.generateKey());

        Heartbeat.thisInstanceInfo = thisInstanceInfo;
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
