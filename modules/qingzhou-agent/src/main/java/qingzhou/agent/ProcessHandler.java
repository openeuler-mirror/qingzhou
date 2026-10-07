package qingzhou.agent;

import java.nio.charset.StandardCharsets;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.dto.Constants;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

class ProcessHandler {
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

        Cipher cipher;
        byte[] requestData;
        try {
            cipher = crypto.getCipher(thisInstanceInfo.getKey());
            // seal/open 内部以时间戳+随机数作为 AAD：时间窗拒绝过期报文，随机数去重拒绝重放；
            // 上传分片以 URL 中的上传键作为绑定上下文，防止分片被重放到其他上传
            String uploadKey = httpRequest.getParameter(Constants.AGENT_UPLOAD_FILE_KEY);
            byte[] context = uploadKey == null ? null : uploadKey.getBytes(StandardCharsets.UTF_8);
            requestData = crypto.getReplayGuardCipher(cipher).decrypt(requestBody, context);
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
