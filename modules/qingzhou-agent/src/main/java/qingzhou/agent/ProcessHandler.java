package qingzhou.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

class ProcessHandler {
    static void handle(HttpRequest httpRequest, HttpResponse httpResponse, Crypto crypto, ProcessHandlerCallback callback) {
        byte[] requestBody = httpRequest.getBody();
        if (requestBody.length == 0) return;

        InstanceInfo thisInstanceInfo = Heartbeat.thisInstanceInfo;
        if (thisInstanceInfo == null) return; // Agent 尚未注册

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

    interface ProcessHandlerCallback {
        String doProcess(byte[] requestData) throws Throwable;
    }
}
