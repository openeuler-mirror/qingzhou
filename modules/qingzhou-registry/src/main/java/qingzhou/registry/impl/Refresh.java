package qingzhou.registry.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

@Component(property = {HttpHandler.HANDLE_PATH + "=/refresh", HttpHandler.HANDLE_NO_AUTH + "=true", HttpHandler.HANDLE_METHODS + "=POST"},
        configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Refresh implements HttpHandler {
    static final Object REFRESH_KEY_LOCK = new Object();

    @Reference
    private Crypto crypto;
    @Reference
    private Logger logger;
    @Reference
    private Registry registry;

    private PairCipher pairCipher;

    @Activate
    public void start(Map<String, String> config) throws Exception {
        String privateKey = config.get("private_key");
        if (privateKey == null || privateKey.trim().isEmpty()) {
            return;
        }
        String decrypt = crypto.getGlobalCipher().tryDecrypt(privateKey, "qingzhou-registry.private_key");
        pairCipher = crypto.getPairCipher(null, decrypt);
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        if (pairCipher == null) {
            httpResponse.status(500).finish();
            return;
        }
        synchronized (REFRESH_KEY_LOCK) {
            handleInternal(httpRequest, httpResponse);
        }
    }

    private void handleInternal(HttpRequest httpRequest, HttpResponse httpResponse) {
        String decryptedRequest = Register.decryptRequest(httpRequest, httpResponse, pairCipher);
        if (decryptedRequest == null) return; // decryptRequest 已写入错误响应

        String[] split = decryptedRequest.split(",", 5);
        if (split.length < 3) {
            httpResponse.status(400).finish();
            return;
        }
        String instanceId = split[0];
        String newKey = split[1];
        String oldKey = split[2];
        InstanceInfo instanceInfo = registry.getRemoteInstance(instanceId);
        if (instanceInfo == null) {
            httpResponse.status(400).finish();
            return;
        }

        // 必须证明持有当前共享密钥：registry 公钥会被分发给所有 agent，不是秘密，
        // 仅凭「能用公钥加密」不足以授权改密钥
        if (!MessageDigest.isEqual(instanceInfo.getKey().getBytes(StandardCharsets.UTF_8),
                oldKey.getBytes(StandardCharsets.UTF_8))) { // 常量时间比较，防时序侧信道
            httpResponse.status(400).finish(); // 与其它失败同状态码，避免成为实例存在性的判别信号
            return;
        }

        byte[] encrypt;
        try {
            Cipher cipher = crypto.getCipher(oldKey);
            encrypt = cipher.encrypt(Boolean.TRUE.toString().getBytes(StandardCharsets.UTF_8));

            instanceInfo.setKey(newKey); // 后续：更新共享的对称密钥，以保障前向安全！
            instanceInfo.setLastRefreshTime(System.currentTimeMillis()); // 更新刷新时间
            // 记录旧钥供宽限期内 invoke 继续加密：agent 要收到本响应才切新钥，
            // 期间 registry 用新钥加密必然解密失败（RTT 窗口）
            ((RegistryImpl) registry).keyRotation.onRotated(instanceId, oldKey);

            httpResponse.send(encrypt).finish();
        } catch (Exception e) {
            logger.error("refresh failed", e);
            httpResponse.status(500).finish();
        }
    }
}
