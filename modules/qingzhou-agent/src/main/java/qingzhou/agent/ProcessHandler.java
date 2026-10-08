package qingzhou.agent;

import java.nio.charset.StandardCharsets;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.ReplayGuardException;
import qingzhou.dto.Constants;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.logger.Logger;

class ProcessHandler {
    static void handle(HttpRequest httpRequest, HttpResponse httpResponse, Crypto crypto, Logger logger,
                       ProcessHandlerCallback callback) {
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
        } catch (ReplayGuardException e) {
            // 报文由请求方控制：格式/时间窗回 400，重放回 409（冲突）便于发送方区分「已受理过」
            if (e.getReason() == ReplayGuardException.Reason.REPLAY) {
                logger.warn("agent request replayed");
                httpResponse.status(409).finish();
            } else {
                logger.warn("invalid agent request: " + e.getMessage());
                httpResponse.status(400).finish();
            }
            return;
        } catch (Exception e) {
            logger.error("agent request processing error", e);
            httpResponse.status(500).finish();
            return;
        }

        // 处理业务，得到响应数据
        String responseData;
        try {
            responseData = callback.doProcess(requestData);
        } catch (Throwable e) {
            logger.error("agent business processing error", e);
            httpResponse.status(500).finish();
            return;
        }

        // 加密响应数据
        byte[] encrypt;
        try {
            encrypt = cipher.encrypt(responseData.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("agent response encryption error", e);
            httpResponse.status(500).finish();
            return;
        }
        httpResponse.send(encrypt).finish();
    }

    interface ProcessHandlerCallback {
        String doProcess(byte[] requestData) throws Throwable;
    }
}
