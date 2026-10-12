package qingzhou.agent;

import java.nio.charset.StandardCharsets;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.ReplayGuardCipher;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.logger.Logger;

class ProcessHandler {
    /**
     * @param context AAD 绑定上下文，由调用方按自身契约提供（可为 null）：报文无法被重放到其他绑定上下文。
     *                本类是各端点共用的传输层，不感知任何具体端点的参数来源
     */
    static void handle(HttpRequest httpRequest, HttpResponse httpResponse, Crypto crypto, Logger logger,
                       byte[] context, ProcessHandlerCallback callback) {
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

        Cipher cipher = null;
        byte[] requestData = null;
        try {
            Cipher primary = crypto.getCipher(thisInstanceInfo.getKey());
            try {
                requestData = decrypt(crypto, primary, requestBody, context);
                cipher = primary;
            } catch (ReplayGuardCipher.ReplayGuardException e) {
                // 报文由请求方控制：格式/时间窗回 400，重放回 409（冲突）便于发送方区分「已受理过」
                if (e.getReason() == ReplayGuardCipher.ReplayGuardException.Reason.REPLAY) {
                    logger.warn("agent request replayed");
                    httpResponse.status(409).finish();
                } else {
                    logger.warn("invalid agent request: " + e.getMessage());
                    httpResponse.status(400).finish();
                }
                return;
            } catch (Exception primaryFailure) {
                // 密钥轮换宽限：registry 宽限期内仍用旧钥加密，回退旧钥重试
                try {
                    String fallbackKey = Heartbeat.decryptKeyFallback();
                    if (fallbackKey != null) {
                        Cipher fallback = crypto.getCipher(fallbackKey);
                        requestData = decrypt(crypto, fallback, requestBody, context);
                        cipher = fallback;
                    }
                } catch (Exception ignored) {
                }
                if (cipher == null) {
                    throw primaryFailure;
                }
            }
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

    private static byte[] decrypt(Crypto crypto, Cipher cipher, byte[] requestBody, byte[] context) throws Exception {
        return crypto.getReplayGuardCipher(cipher).decrypt(requestBody, context);
    }

    interface ProcessHandlerCallback {
        String doProcess(byte[] requestData) throws Throwable;
    }
}
