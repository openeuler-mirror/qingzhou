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

@Component(property = HttpHandler.HANDLE_PATH + "=/refresh",
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
        String instanceId = split[0];
        String newKey = split[1];
        InstanceInfo instanceInfo = registry.getRemoteInstance(instanceId);
        if (instanceInfo == null) return;

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
