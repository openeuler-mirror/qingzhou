package qingzhou.agent;

import java.nio.charset.StandardCharsets;

import qingzhou.crypto.Crypto;
import qingzhou.crypto.EpochPacketCipher;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.logger.Logger;

class ProcessHandler {
    /**
     * @param aad AAD 绑定上下文，由调用方按自身契约提供（可为 null）：报文无法被重放到其他绑定上下文。
     *            本类是各端点共用的传输层，不感知任何具体端点的参数来源
     */
    static void handle(HttpRequest httpRequest, HttpResponse httpResponse, Crypto crypto, Logger logger,
                       String aad, ProcessHandlerCallback callback) {
        byte[] requestBody = httpRequest.getBody(); // 空报文由解密环节按无效报文返回 400

        InstanceInfo thisInstanceInfo = Heartbeat.currentInstanceInfo();
        if (thisInstanceInfo == null) {
            httpResponse.status(503);
            return;
        }

        EpochPacketCipher packetCipher;
        byte[] requestData;
        try {
            // AAD 绑定 域名 + 目标实例 ID（+ 端点上下文）：同域不同 agent 的报文无法互相挪用
            packetCipher = crypto.getEpochPacketCipher(Heartbeat.rootKey(), thisInstanceInfo.getDomain());
            requestData = packetCipher.decrypt(requestBody, crypto.generateCipherAad(
                    thisInstanceInfo.getDomain(), thisInstanceInfo.getId(), aad));
        } catch (Exception e) {
            // 报文由请求方控制：格式、时间窗、重放与密钥失配一律 400，避免成为配置状态的判别信号
            logger.warn("agent request decrypt failed: " + e.getMessage());
            httpResponse.status(400);
            return;
        }

        // 处理业务，得到响应数据
        String responseData;
        try {
            responseData = callback.doProcess(requestData);
        } catch (Throwable e) {
            logger.error("agent business processing error", e);
            httpResponse.status(500);
            return;
        }

        // 加密响应数据
        byte[] encrypt;
        try {
            // 响应绑定：以请求明文的 SHA-256 作为响应 GCM 的 AAD，
            // 旧响应无法被重放为对其他请求的有效应答（请求不同则 AAD 不匹配，解密失败）
            byte[] responseAad = crypto.getMessageDigest().sha256(requestData);
            encrypt = packetCipher.encrypt(responseData.getBytes(StandardCharsets.UTF_8), responseAad);
        } catch (Exception e) {
            logger.error("agent response encryption error", e);
            httpResponse.status(500);
            return;
        }
        httpResponse.send(encrypt);
    }

    interface ProcessHandlerCallback {
        String doProcess(byte[] requestData) throws Throwable;
    }
}
