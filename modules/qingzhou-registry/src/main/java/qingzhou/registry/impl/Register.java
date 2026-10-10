package qingzhou.registry.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import org.osgi.service.component.annotations.*;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.PairCipher;
import qingzhou.dto.Constants;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=/register", HttpHandler.HANDLE_NO_AUTH + "=true", HttpHandler.HANDLE_METHODS + "=POST"},
        configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Register implements HttpHandler {
    private static final int MAX_REGISTER_BODY = 16 * 1024; // 注册/刷新报文上限
    /**
     * 注册报文时间戳允许的时钟偏移：超窗即拒绝，旧报文无法重放
     */
    private static final long MAX_CLOCK_SKEW_MILLIS = 2 * 60 * 1000;

    static String decryptRequest(HttpRequest httpRequest, HttpResponse httpResponse, PairCipher pairCipher) {
        byte[] requestBody = httpRequest.getBody();
        if (requestBody == null || requestBody.length == 0) {
            httpResponse.status(400).finish();
            return null;
        }
        // 私钥解密按模长分块，成本随报文线性增长；注册/刷新报文实际仅数 KB，先设硬上限防 CPU DoS
        if (requestBody.length > MAX_REGISTER_BODY) {
            httpResponse.status(413).finish();
            return null;
        }

        byte[] decrypted;
        try {
            decrypted = pairCipher.decryptWithPrivateKey(requestBody);
        } catch (Exception e) {
            httpResponse.status(400).send("key auth error").finish();
            return null;
        }

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    @Reference
    private Logger logger;
    @Reference
    private Crypto crypto;
    @Reference
    private Registry registry;
    @Reference
    private Json json;

    private PairCipher pairCipher;
    private byte[] registerToken;
    private Timer timer;

    @Activate
    public void start(Map<String, String> config) throws Exception {
        String privateKey = config.get("private_key");
        if (privateKey == null || privateKey.trim().isEmpty()) {
            logger.warn("'private_key' is not configured, remote instance registration feature is unavailable.");
            return;
        }
        privateKey = crypto.getGlobalCipher().tryDecrypt(privateKey, "qingzhou-registry.private_key");
        pairCipher = crypto.getPairCipher(null, privateKey);

        String registerTokenConfig = config.get("register_token");
        if (registerTokenConfig == null || registerTokenConfig.trim().isEmpty()) {
            logger.warn("'register_token' is not configured, remote instance registration is unavailable.");
            return;
        } else {
            registerToken = registerTokenConfig.trim().getBytes(StandardCharsets.UTF_8);
        }

        long interval = 1000 * Long.parseLong(config.getOrDefault("interval", "6"));
        long timeout = 1000 * Long.parseLong(config.getOrDefault("timeout", "12"));
        timer = new Timer("registry-health-check");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (Refresh.REFRESH_KEY_LOCK) {
                    Set<String> toRemove = new HashSet<>();
                    for (String next : registry.getAllRemoteInstances()) {
                        InstanceInfo remoteInstance = registry.getRemoteInstance(next);
                        if (remoteInstance != null) {
                            long lastRefreshTime = remoteInstance.getLastRefreshTime();
                            if (lastRefreshTime + timeout < System.currentTimeMillis()) {
                                toRemove.add(next);
                            }
                        }
                    }
                    toRemove.forEach(s -> ((RegistryImpl) registry).removeRemoteApps(s));
                }
            }
        }, interval, interval);
    }

    @Deactivate
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        if (pairCipher == null || registerToken == null) {
            httpResponse.status(500).finish();
            return;
        }
        synchronized (Refresh.REFRESH_KEY_LOCK) {
            handle0(httpRequest, httpResponse);
        }
    }

    private void handle0(HttpRequest httpRequest, HttpResponse httpResponse) {
        String decryptedRequest = decryptRequest(httpRequest, httpResponse, pairCipher);
        if (decryptedRequest == null) return;

        // 报文：registerToken + timestamp + nonce + currentKeyProof + InstanceInfoJson
        String[] parts = decryptedRequest.split(Constants.AGENT_REGISTER_TOKEN_SP, 5);
        if (!isValid(parts, httpResponse)) return;

        InstanceInfo instanceInfo;
        try {
            instanceInfo = json.fromJson(parts[4], InstanceInfo.class);
            instanceInfo.setHost(httpRequest.getRemoteHost());
        } catch (Exception e) {
            httpResponse.send("data format error").finish();
            return;
        }

        // 重复注册必须证明持有当前实例密钥：证明 = 用当前密钥加密报文 nonce，GCM 认证通过即证明持钥。
        // 否则重放的旧注册包会把共享密钥回滚到旧值，并伪造存活时间
        InstanceInfo exists = registry.getRemoteInstance(instanceInfo.getId());
        if (exists != null && !proofMatchesCurrentKey(exists.getKey(), parts[3], parts[2])) {
            logger.warn("registration rejected: stale registration for instance: " + instanceInfo.getId());
            httpResponse.status(409).send("stale registration").finish();
            return;
        }

        RegistryImpl registryImpl = (RegistryImpl) registry;
        registryImpl.addRemoteInstanceInfo(instanceInfo);

        String msg;
        if (exists != null) {
            msg = "Redundant.";
        } else {
            List<AppMeta> appMetas = instanceInfo.getAppMetas();
            msg = appMetas.size() + " apps accepted.";
            for (AppMeta appMeta : appMetas) {
                logger.info(String.format("registration, instance: %s, app: %s", instanceInfo.getId(), appMeta.getApp().code));
            }
        }

        String instanceKey = instanceInfo.getKey();
        try {
            Cipher cipher = crypto.getCipher(instanceKey);
            byte[] encrypt = cipher.encrypt(msg.getBytes(StandardCharsets.UTF_8));
            httpResponse.send(encrypt).finish();
        } catch (Exception e) {
            httpResponse.status(500).finish();
            logger.error("encryption failed, key len: " + instanceKey.length());
        }
    }

    private boolean isValid(String[] parts, HttpResponse httpResponse) {
        if (parts.length < 5) {
            httpResponse.send("data format error").finish();
            return false;
        }

        // 注册授权：注册令牌为预共享秘密，须先于任何写入校验；registry 公钥分发给所有 agent、不是秘密，
        // 仅凭「能用公钥加密」不足以授权注册。令牌校验通过后，新实例与重复注册（替换）均受其保护。
        boolean isAuthorized = registerToken != null && MessageDigest.isEqual(registerToken, parts[0].getBytes(StandardCharsets.UTF_8));
        if (!isAuthorized) {
            httpResponse.send("register_token error").finish();
            return false;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            httpResponse.send("data format error").finish();
            return false;
        }
        // 时间窗：重放旧报文的时间戳必然过期，在任何状态变更之前拒绝
        if (Math.abs(System.currentTimeMillis() - timestamp) > MAX_CLOCK_SKEW_MILLIS) {
            httpResponse.status(400).send("stale registration").finish();
            return false;
        }

        // nonce 去重：拦截时间窗内的精确重放。登记须先于任何状态变更
        if (!crypto.getNonceRegistry().registerIfAbsent(parts[2], timestamp, MAX_CLOCK_SKEW_MILLIS * 2)) {
            httpResponse.status(409).send("replay").finish();
            return false;
        }

        return true;
    }

    // 重复注册的持有证明：proof = agent 用当前实例密钥加密的报文 nonce。
    // 用 registry 侧当前密钥解密并比对 nonce，GCM 认证通过即证明请求方持有当前密钥
    private boolean proofMatchesCurrentKey(String currentKey, String proof, String nonce) {
        try {
            Cipher cipher = crypto.getCipher(currentKey);
            String decrypted = cipher.decrypt(proof);
            return MessageDigest.isEqual(decrypted.getBytes(StandardCharsets.UTF_8), nonce.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false; // 解密失败（密钥不匹配/证明被篡改）一律视为未证明
        }
    }
}
