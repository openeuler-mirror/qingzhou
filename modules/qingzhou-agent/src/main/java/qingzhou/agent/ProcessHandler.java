package qingzhou.agent;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

import static qingzhou.crypto.Cipher.IV_SIZE;

class ProcessHandler {
    private static final int SEEN_IV_LIMIT = 10_000; // 去重表容量上限：只保留近期 IV，过期后重放已无意义

    // 已受理请求的 IV（LRU）：AES-GCM 每次加密随机生成 IV，重放的密文 IV 必然重复
    private static final Map<String, Long> SEEN_IV = new LinkedHashMap<String, Long>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > SEEN_IV_LIMIT; // 有界：超出即淘汰最久未使用的一条
        }
    };

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

        // 重放判定先于解密：IV 重复即丢弃，避免为重复请求付出解密成本。
        // 原子登记，使并发重放的同一密文只有一个能继续。
        String iv = Base64.getEncoder().encodeToString(Arrays.copyOf(requestBody, IV_SIZE));
        if (!registerIv(iv)) {
            httpResponse.status(409).finish(); // 冲突：发送方据此区分「已受理过」与「解密失败」
            return;
        }

        // 解密，得到请求数据
        Cipher cipher;
        byte[] requestData;
        try {
            cipher = crypto.getCipher(thisInstanceInfo.getKey());
            requestData = cipher.decrypt(requestBody);
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

    // 原子登记：并发重放的同一密文只有一个能登记成功，其余判为重放
    private static synchronized boolean registerIv(String iv) {
        return SEEN_IV.put(iv, System.currentTimeMillis()) == null;
    }

    interface ProcessHandlerCallback {
        String doProcess(byte[] requestData) throws Throwable;
    }
}
