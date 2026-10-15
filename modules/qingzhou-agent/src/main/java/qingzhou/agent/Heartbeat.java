package qingzhou.agent;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.*;
import qingzhou.common.Configs;
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

    // agent 侧保留旧钥的窗口放宽到注册端宽限期的两倍：覆盖"注册端宽限期内仍用旧钥加密"的整个窗口，
    // 并对两侧时钟偏差 / 网络抖动留余量，避免窗口边缘的解密失败
    private static final long AGENT_KEY_GRACE_MILLIS_DECRYPT = Constants.AGENT_KEY_GRACE_MILLIS * 2;

    /**
     * 宽限期内返回轮换前的旧钥，供 ProcessHandler 解密回退；过期或无轮换返回 null
     */
    static String decryptKeyFallback() {
        String key = previousKey;
        if (key == null) return null;

        if (System.currentTimeMillis() - previousKeyAt <= AGENT_KEY_GRACE_MILLIS_DECRYPT) {
            return key;
        } else {
            previousKey = null;
            return null;
        }
    }

    @Activate
    public void start(Map<String, String> config) throws Exception {
        Configs configs = new Configs(config);
        qzVersion = configs.get("qingzhou.version"); // 缓存，防止系统参数被应用覆盖
        pairCipher = crypto.getPairCipher(configs.notBlank("public_key"), null);
        registerToken = configs.notBlank("register_token");

        String registryUrl = configs.notBlank("url");
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
                                registerToken,
                                String.valueOf(System.currentTimeMillis()),
                                nonce,
                                currentKeyProof,
                                json.toJson(thisInstanceInfo));
                        String registration = send(registerUrl, registerData.getBytes(StandardCharsets.UTF_8));
                        logger.info("instance registered, response length=" + (registration == null ? 0 : registration.length()));
                    }
                } catch (Exception e) {
                    logger.error("heartbeat service error", e);
                }
            }
        }, 1000 * 10, 1000L * configs.atLeast("interval", 2, 6));
    }

    @Deactivate
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private boolean appChanged() throws Exception {
        InstanceInfo instanceInfo = thisInstanceInfo;
        if (instanceInfo == null) return true;

        List<AppMeta> storedAppMetas = new ArrayList<>(instanceInfo.getAppMetas()); // 拷贝后排序，不改动被 Netty 线程读取的共享列表
        List<AppMeta> currentAppMetas = currentAppMetas();
        if (storedAppMetas.size() != currentAppMetas.size()) return true;

        sortByCode(storedAppMetas); // 固定顺序，避免指纹计算错误
        sortByCode(currentAppMetas); // 固定顺序，避免指纹计算错误
        for (int i = 0; i < storedAppMetas.size(); i++) {
            // 按内容比较而非引用：AppMeta 的默认值等字段可能被就地修改而引用不变
            if (!json.toJson(storedAppMetas.get(i)).equals(json.toJson(currentAppMetas.get(i)))) return true;
        }

        return false;
    }

    private String send(String url, byte[] data) {
        Response response = null;
        try {
            byte[] encrypted = pairCipher.encryptWithPublicKey(data);
            // 显式 POST：请求必须带密文正文，且 registry 侧已限定 POST
            response = httpClient.send(
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
        } finally {
            // 释放底层连接资源，避免每次心跳累积泄漏
            if (response != null) response.cancel();
        }
        return null;
    }

    private static void sortByCode(List<AppMeta> list) {
        list.sort(Comparator.comparing(appMeta -> appMeta.getApp().code));
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
        InstanceInfo instanceInfo = new InstanceInfo();

        // 实例部署的应用
        List<AppMeta> currentAppMetas = currentAppMetas();
        sortByCode(currentAppMetas); // 固定顺序，避免指纹计算错误
        instanceInfo.getAppMetas().addAll(currentAppMetas);

        // 实例版本
        instanceInfo.setVersion(qzVersion);

        // 实例端口
        Dictionary<String, Object> httpServerConfig = configAdmin.getConfiguration("qingzhou-http-server", null).getProperties();
        if (httpServerConfig == null) {
            throw new IllegalStateException("qingzhou-http-server configuration is missing");
        }
        Object portValue = httpServerConfig.get("port");
        if (portValue == null) {
            throw new IllegalStateException("qingzhou-http-server configuration is missing 'port'");
        }
        int serverPort = Integer.parseInt(String.valueOf(portValue));
        instanceInfo.setPort(serverPort);

        boolean sslEnabled = Boolean.parseBoolean(String.valueOf(httpServerConfig.get("ssl_enabled")));
        instanceInfo.setSslEnabled(sslEnabled);

        // 所有信息收集完成后，计算实例 ID，同一个实例多次重启的 ID 不应该变化
        String digestTemp = json.toJson(instanceInfo);
        digestTemp += ips(); // 集群下的多实例，除了IP别的都是一样的，加入 IP 标识可区分集群中的不同实例
        String uniqueDataId = crypto.getMessageDigest().md5(digestTemp);
        instanceInfo.setId(uniqueDataId);

        // 实例密钥，动态生成的，放在 ID 计算之前，不参与 ID 计算
        instanceInfo.setKey(crypto.generateKey());

        Heartbeat.thisInstanceInfo = instanceInfo;
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
