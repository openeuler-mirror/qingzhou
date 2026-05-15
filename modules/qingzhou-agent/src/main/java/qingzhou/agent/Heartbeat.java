package qingzhou.agent;

import java.io.File;
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
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.HttpResult;
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
    private String refreshUrl;
    private String registerUrl;
    private Timer timer;
    private String qzVersion;

    static volatile InstanceInfo thisInstanceInfo;

    @Activate
    public void start(Map<String, String> config) throws Exception {
        qzVersion = System.getProperty("qingzhou.version"); // 缓存，防止系统参数被应用覆盖
        qzVersion = new File(qzVersion).getName().substring("version".length());

        pairCipher = crypto.getPairCipher(config.get("public_key"), null);

        String registryUrl = config.get("url");
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
                    String refreshInfos = thisInstanceInfo.getId() + "," + newKey;
                    String refreshed = send(refreshUrl, refreshInfos.getBytes(StandardCharsets.UTF_8));
                    if (Boolean.parseBoolean(refreshed)) { // 服务端已经刷新了密钥
                        thisInstanceInfo.setKey(newKey);
                    } else {
                        String registerData = json.toJson(thisInstanceInfo);
                        String registration = send(registerUrl, registerData.getBytes(StandardCharsets.UTF_8));
                        logger.info("registration response: " + registration);
                    }
                } catch (Exception e) {
                    logger.error("heartbeat service error", e);
                }
            }
        }, 2000, 1000 * Long.parseLong(config.get("interval")));
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
            HttpResult response = httpClient.request(url, HttpMethod.POST, encrypted, null);
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
