package qingzhou.agent;

import java.nio.charset.StandardCharsets;

import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainKeys;

abstract class AbstractAgentHandler implements HttpHandler {
    protected AgentDomainKeys agentDomainKeys;
    protected Crypto crypto;
    protected Logger logger;

    @Reference
    private LocalAgentDomainKey localAgentDomainKey;

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        InstanceInfo thisInstanceInfo = Heartbeat.currentInstanceInfo();
        if (thisInstanceInfo == null) {
            logger.error("agent instance info is not initialized");
            httpResponse.status(503); // 内部环境未准备好，服务器问题：503
            return;
        }

        EpochKeyRotatingCipher agentCipher = localAgentDomainKey.getLocalAgentCipher();
        if (agentCipher == null) {
            httpResponse.status(503);
            return;
        }

        byte[] requestBody = httpRequest.getBody(); // 空报文由解密环节按无效报文返回 400
        if (requestBody == null || requestBody.length == 0) {
            httpResponse.status(400);
            return;
        }

        byte[] requestData = null;
        try {
            byte[] agentAad = agentDomainKeys.toAgentAad(thisInstanceInfo.getId(), aadName());
            requestData = agentCipher.decrypt(requestBody, agentAad);
        } catch (Exception e) {
            // 报文由请求方控制：格式、时间窗、重放与密钥失配一律 400，避免成为配置状态的判别信号
            logger.warn("agent request decrypt failed: " + e.getMessage());
        }
        if (requestData == null || requestData.length == 0) {
            httpResponse.status(400);
            return;
        }

        // 处理业务，得到响应数据
        String responseData;
        try {
            responseData = doProcess(requestData, httpRequest);
        } catch (Throwable e) {
            logger.error("agent business processing error", e);
            httpResponse.status(500);
            return;
        }

        // 加密响应数据
        try {
            byte[] agentAad = agentDomainKeys.toRegistryAad(thisInstanceInfo.getId(), aadName());
            byte[] encrypt = agentCipher.encrypt(responseData.getBytes(StandardCharsets.UTF_8), agentAad);
            httpResponse.send(encrypt);
        } catch (Exception e) {
            logger.error("agent response encryption error", e);
            httpResponse.status(500);
        }
    }

    protected abstract String aadName();

    protected abstract String doProcess(byte[] requestData, HttpRequest httpRequest) throws Throwable;
}
