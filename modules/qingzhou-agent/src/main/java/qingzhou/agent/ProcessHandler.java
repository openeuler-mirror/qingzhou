package qingzhou.agent;

import java.nio.charset.StandardCharsets;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

import static qingzhou.crypto.Cipher.IV_SIZE;

class ProcessHandler {
    static void handle(HttpRequest httpRequest, HttpResponse httpResponse, Crypto crypto, ProcessHandlerCallback callback) {
        byte[] requestBody = httpRequest.getBody();
        if (requestBody.length == 0) {
            httpResponse.status(400).finish();
            return;
        }

        InstanceInfo thisInstanceInfo = Heartbeat.thisInstanceInfo;
        if (thisInstanceInfo == null) {
            httpResponse.status(503).finish();
            return;
        }

        if (requestBody.length <= IV_SIZE) { // 长度校验须先于 IV 提取，防止数组越界
            httpResponse.status(500).finish(); // 与解密失败同文案，避免成为判别信号
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

    interface ProcessHandlerCallback {
        String doProcess(byte[] requestData) throws Throwable;
    }
}
