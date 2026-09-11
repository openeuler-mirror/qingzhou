package qingzhou.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

class ProcessHandler {
    private static final int IV_SIZE = 12; // 与 CipherImpl 的 AES-GCM IV 长度一致
    private static final Map<String, Long> SEEN_IV = new ConcurrentHashMap<>();

    static void handle(HttpRequest httpRequest, HttpResponse httpResponse, Crypto crypto, ProcessHandlerCallback callback) {
        byte[] requestBody = httpRequest.getBody();
        if (requestBody.length == 0) return;

        InstanceInfo thisInstanceInfo = Heartbeat.thisInstanceInfo;
        if (thisInstanceInfo == null) return; // Agent 尚未注册

        // AES-GCM 每次加密都生成新的随机 IV，重放的密文 IV 必然重复，据此在解密前就丢弃
        String iv = ivOf(crypto, requestBody);
        if (requestBody.length <= IV_SIZE || SEEN_IV.containsKey(iv)) {
            httpResponse.status500Finish("key auth error"); // 与解密失败同文案，避免成为判别信号
            return;
        }

        // 解密，得到请求数据
        Cipher cipher;
        byte[] requestData;
        try {
            cipher = crypto.getCipher(thisInstanceInfo.getKey());
            requestData = cipher.decrypt(requestBody);
        } catch (Exception e) {
            httpResponse.status500Finish("key auth error");
            return;
        }
        rememberIv(iv); // 解密成功才登记，防止无效密文撑爆去重表

        // 处理业务，得到响应数据
        String responseData;
        try {
            responseData = callback.doProcess(requestData);
        } catch (Throwable e) {
            // 业务异常可回显以便代理定位；NPE 的 Helpful 消息含内部类结构、IO 异常含服务器绝对路径，均不回显
            String msg = e instanceof NullPointerException || e instanceof IOException ? null : e.getMessage();
            httpResponse.status500Finish(msg != null ? msg : "business processing error");
            return;
        }

        // 加密响应数据
        byte[] encrypt;
        try {
            encrypt = cipher.encrypt(responseData.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            httpResponse.status500Finish("encryption failed");
            return;
        }
        httpResponse.sendFinish(encrypt);
    }

    private static String ivOf(Crypto crypto, byte[] body) {
        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(body, 0, iv, 0, IV_SIZE);
        return crypto.getBase64Coder().encode(iv);
    }

    private static void rememberIv(String iv) {
        if (SEEN_IV.size() > 10_000) { // 惰性清理过期记录，防止重放洪水导致内存膨胀
            long now = System.currentTimeMillis();
            long IV_TTL_MILLIS = 5 * 60 * 1000;
            SEEN_IV.entrySet().removeIf(entry -> now - entry.getValue() > IV_TTL_MILLIS);
        }
        SEEN_IV.put(iv, System.currentTimeMillis());
    }

    interface ProcessHandlerCallback {
        String doProcess(byte[] requestData) throws Throwable;
    }
}
