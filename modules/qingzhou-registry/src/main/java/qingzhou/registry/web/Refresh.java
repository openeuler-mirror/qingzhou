package qingzhou.registry.web;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.PairCipher;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.logger.Logger;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=/refresh", HttpHandler.HANDLE_NO_AUTH + "=true"},
        configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Refresh implements HttpHandler {
    public static final Object REFRESH_KEY_LOCK = new Object();

    @Reference
    private Crypto crypto;
    @Reference
    private Logger logger;
    @Reference
    private Registry registry;

    private PairCipher pairCipher;

    @Activate
    public void start(Map<String, String> config) throws Exception {
        pairCipher = crypto.getPairCipher(null, config.get("private_key"));
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        synchronized (REFRESH_KEY_LOCK) {
            handle0(httpRequest, httpResponse);
        }
    }

    private void handle0(HttpRequest httpRequest, HttpResponse httpResponse) {
        String decryptedRequest = Register.decryptRequest(httpRequest, httpResponse, pairCipher);
        if (decryptedRequest == null) return;

        String[] split = decryptedRequest.split(",");
        if (split.length < 3) {
            logger.warn("malformed refresh request"); // 旧版 agent 只发两段，升级时需同步
            return;
        }
        String instanceId = split[0];
        String newKey = split[1];
        String oldKey = split[2];
        InstanceInfo instanceInfo = registry.getRemoteInstance(instanceId);
        if (instanceInfo == null) return;

        // 必须证明持有当前共享密钥：registry 公钥会被分发给所有 agent，不是秘密，
        // 仅凭「能用公钥加密」不足以授权改密钥
        if (!instanceInfo.getKey().equals(oldKey)) {
            logger.warn("refresh request rejected: key proof mismatch, instance: " + instanceId);
            return;
        }

        String instanceKey = instanceInfo.getKey();
        byte[] encrypt;
        try {
            Cipher cipher = crypto.getCipher(instanceKey);
            encrypt = cipher.encrypt(Boolean.TRUE.toString().getBytes(StandardCharsets.UTF_8));
            instanceInfo.setKey(newKey); // 后续：更新共享的对称密钥，以保障前向安全！
            instanceInfo.setLastRefreshTime(System.currentTimeMillis()); // 更新刷新时间
        } catch (Exception e) {
            httpResponse.status500Finish("instance key error");
            logger.error("encryption failed, key len: " + instanceKey.length());
            return;
        }
        httpResponse.sendFinish(encrypt);
    }
}
