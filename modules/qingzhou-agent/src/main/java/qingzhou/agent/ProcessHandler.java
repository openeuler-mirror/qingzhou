package qingzhou.agent;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.ReplayGuard;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

import static qingzhou.crypto.Cipher.IV_SIZE;

class ProcessHandler {
    private static final int SEEN_IV_LIMIT = 10_000; // 内存兜底：容量不是安全边界，安全边界是密钥轮换

    // 旧格式报文的已受理 IV（LRU）：AES-GCM 每次加密随机生成 IV，重放的密文 IV 必然重复。
    // 生命周期不超过实例密钥：密钥轮换后旧密文已无法解密，去重表随之清空。
    // 新格式报文改用 ReplayGuard（时间戳+随机数 AAD），不再依赖本表。
    private static final Map<String, Long> SEEN_IV = new LinkedHashMap<String, Long>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > SEEN_IV_LIMIT;
        }
    };
    private static String seenIvKey;

    static void handle(HttpRequest httpRequest, HttpResponse httpResponse, Crypto crypto, ProcessHandlerCallback callback) {
        byte[] requestBody = httpRequest.getBody();
        if (requestBody == null || requestBody.length == 0) {
            httpResponse.status(400).finish();
            return;
        }

        InstanceInfo thisInstanceInfo = Heartbeat.thisInstanceInfo;
        if (thisInstanceInfo == null) {
            httpResponse.status(503).finish();
            return;
        }

        if (requestBody.length <= IV_SIZE) { // 长度校验须先于 IV 提取，防止数组越界
            httpResponse.status(400).finish(); // 与解密失败同状态码，避免成为判别信号
            return;
        }

        Cipher cipher;
        byte[] requestData;
        try {
            cipher = crypto.getCipher(thisInstanceInfo.getKey());
            // 新格式：时间戳+随机数作为 AAD 参与认证，兼具时效判定与去重键
            requestData = ReplayGuard.openRequest(cipher, requestBody);
            if (requestData == null) {
                // 旧格式（升级期）：无时间戳，只能按 IV 去重
                String iv = Base64.getEncoder().encodeToString(Arrays.copyOf(requestBody, IV_SIZE));
                if (!registerIv(iv, thisInstanceInfo.getKey())) {
                    httpResponse.status(409).finish(); // 冲突：发送方据此区分「已受理过」与「解密失败」
                    return;
                }
                requestData = cipher.decrypt(requestBody);
            }
        } catch (Exception e) {
            httpResponse.status(500).finish();
            return;
        }

        // 处理业务，得到响应数据
        String responseData;
        try {
            responseData = callback.doProcess(requestData);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            httpResponse.status(500).finish();
            return;
        }

        // 加密响应数据
        byte[] encrypt;
        try {
            encrypt = cipher.encrypt(responseData.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            httpResponse.status(500).finish();
            return;
        }
        httpResponse.send(encrypt).finish();
    }

    // 原子登记：并发重放的同一密文只有一个能登记成功，其余判为重放；
    // 密钥轮换即整体清空，使去重表生命周期不超过密钥生命周期，避免旧 IV 被挤出后可重放
    private static synchronized boolean registerIv(String iv, String currentKey) {
        if (!Objects.equals(seenIvKey, currentKey)) {
            seenIvKey = currentKey;
            SEEN_IV.clear();
        }
        return SEEN_IV.put(iv, System.currentTimeMillis()) == null;
    }

    interface ProcessHandlerCallback {
        String doProcess(byte[] requestData) throws Throwable;
    }
}
