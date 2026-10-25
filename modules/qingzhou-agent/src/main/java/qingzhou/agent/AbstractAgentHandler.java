package qingzhou.agent;

import java.nio.charset.StandardCharsets;

import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainService;

abstract class AbstractAgentHandler implements HttpHandler {
    protected Logger logger;
    protected AgentDomainService agentDomainService;
    protected AgentCipherManager agentCipherManager;
    protected AgentInstanceManager agentInstanceManager;

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        InstanceInfo cachedInstance = agentInstanceManager.getCachedInstance();
        if (cachedInstance == null) {
            logger.error("agent instance info is not initialized");
            httpResponse.status(503); // 内部环境未准备好，服务器问题：503
            return;
        }

        EpochKeyRotatingCipher instanceCipher = agentCipherManager.getInstanceCipher();
        if (instanceCipher == null) {
            httpResponse.status(503);
            return;
        }

        byte[] requestBody = httpRequest.getBody(); // 空报文直接 400：不入解密环节，避免不可信输入刷出的噪声告警
        if (requestBody == null || requestBody.length == 0) {
            httpResponse.status(400);
            return;
        }

        byte[] requestData = null;
        byte[] agentAad = null;
        try {
            agentAad = agentDomainService.aadForAgent(cachedInstance.getId(), uriName(), aadContext(httpRequest));
            requestData = instanceCipher.decrypt(requestBody, agentAad);
        } catch (Exception e) {
            // 报文由请求方控制：格式、时间窗、重放与密钥失配一律 400，避免成为配置状态的判别信号
            logger.warn("agent request decrypt failed: " + e.getMessage());
        }
        if (requestData == null) {
            httpResponse.status(400);
            return;
        }

        // 处理业务，得到响应数据
        String responseData;
        try {
            responseData = doProcess(requestData, httpRequest);
        } catch (IllegalArgumentException e) { // 请求数据非法：属调用方问题，不该按服务端故障报 500
            logger.warn("agent request rejected: " + e.getMessage());
            httpResponse.status(400);
            return;
        } catch (Throwable e) {
            logger.error("agent business processing error", e);
            httpResponse.status(500);
            return;
        }

        // 加密响应数据
        try {
            byte[] responseAad = agentDomainService.responseAad(agentAad);
            byte[] encrypt = instanceCipher.encrypt(responseData.getBytes(StandardCharsets.UTF_8), responseAad);
            httpResponse.send(encrypt);
        } catch (Exception e) {
            logger.error("agent response encryption error", e);
            httpResponse.status(500);
        }
    }

    protected abstract String uriName();

    /**
     * AAD 附加上下文：对明文携带的控制参数（如上传分片的文件键与偏移）做完整性绑定，
     * 篡改即解密失败。默认只绑定端点名。
     */
    protected String[] aadContext(HttpRequest httpRequest) {
        return null;
    }

    protected abstract String doProcess(byte[] requestData, HttpRequest httpRequest) throws Throwable;
}
