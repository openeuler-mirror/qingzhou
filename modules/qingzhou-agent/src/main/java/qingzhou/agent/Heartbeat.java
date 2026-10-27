package qingzhou.agent;

import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.osgi.service.component.annotations.*;
import qingzhou.common.Configs;
import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainService;
import qingzhou.registry.Constants;

@Component(configurationPid = "qingzhou-agent", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Heartbeat {
    @Reference
    private HttpClient httpClient;
    @Reference
    private Logger logger;
    @Reference
    private Json json;
    @Reference
    private AgentInstanceManager agentInstanceManager;
    @Reference
    private AgentCipherManager agentCipherManager;
    @Reference
    private AgentDomainService agentDomainService;

    private String registerUrl;

    private Timer timer;
    // 是否已被中心登记（当前 instanceId + secret 已上报且中心返回 200/304）
    private boolean registered;

    @Activate
    public void start(Map<String, String> config) {
        Configs configs = new Configs(config);
        String registryUrl = configs.notBlank("url");
        while (registryUrl.endsWith("/")) {
            registryUrl = registryUrl.substring(0, registryUrl.length() - 1);
        }
        registerUrl = registryUrl + "/registry" + Constants.REMOTE_REGISTER_URI;

        timer = new Timer("agent-heartbeat");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (agentInstanceManager.appChanged()) {
                        agentInstanceManager.reCacheInstanceInfo();
                        registered = false; // 新 id + 新 secret，需重新完整注册
                    }

                    send(); // 心跳即注册：注册报文同时承担存活保活（registry 健康检查依据 lastRefreshTime）。
                } catch (Exception e) {
                    logger.error("heartbeat service error", e);
                }
            }
        }, 0L, 1000L * configs.atLeast("interval", 2, 6));
    }

    @Deactivate
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void send() {
        try {
            Payload payload = getPayload();
            String url = registerUrl + "?"
                    + Constants.REMOTE_PARAMETER_DOMAIN_ID + "=" + encode(agentCipherManager.getAgentDomain())
                    + (payload.parameter.isEmpty() ? "" : "&" + payload.parameter);
            byte[] encrypted = payload.cipher.encrypt(payload.data.getBytes(StandardCharsets.UTF_8), payload.aad);
            Response response = httpClient.send(httpClient.buildRequest(url).method(HttpMethod.POST).body(encrypted));

            if (response.getStatus() == 304) {
                registered = true;
                return;
            }

            // 200 只对应新实例登记（续期成功恒为 304）：最小心跳在实例已失效时会被解密拒绝回 400，
            // 走下面的失败分支重置 registered，下一个周期重发完整注册
            if (response.getStatus() != 200) {
                logger.error("response code error [ " + response.getStatus() + " ] : " + registerUrl);
                registered = false;
                return;
            }

            InstanceInfo cachedInstance = agentInstanceManager.getCachedInstance();
            byte[] responseAad = agentDomainService.responseAad(payload.aad);
            byte[] registeredInstanceId = payload.cipher.decrypt(response.getBody(), responseAad);
            cachedInstance.setId(new String(registeredInstanceId, StandardCharsets.UTF_8));
            registered = true;
            logger.info(cachedInstance.getAppMetas().size() + " apps registered");
        } catch (ConnectException e) {
            logger.error("failed to connect to the URL: " + registerUrl + ", msg: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8"); // Java 8 无 Charset 重载
    }

    private Payload getPayload() throws Exception {
        InstanceInfo cachedInstance = agentInstanceManager.getCachedInstance();
        if (registered) { // registrationAad: 有 instanceId 时，用于心跳双向报文的 AAD 计算，无则用于首次注册报文的 AAD 计算
            // 已登记后：续期报文不再序列化 InstanceInfo，且用每实例密钥加密
            // ——既缩小 InstanceInfo.instanceSecret 暴露窗口，又防止同域其他 agent 冒名续期
            return new Payload(agentCipherManager.getInstanceCipher(),
                    json.toJson(Collections.singletonMap("id", cachedInstance.getId())),
                    agentDomainService.aadForRegistry(agentCipherManager.getAgentDomain(), cachedInstance.getId()),
                    Constants.REMOTE_PARAMETER_INSTANCE_ID + "=" + encode(cachedInstance.getId()));
        } else {
            return new Payload(agentCipherManager.getAgentCipher(),
                    json.toJson(cachedInstance),
                    agentDomainService.aadForRegistry(agentCipherManager.getAgentDomain(), null),
                    "");
        }
    }

    private static class Payload {
        final EpochKeyRotatingCipher cipher;
        final String data;
        final byte[] aad;
        final String parameter;

        Payload(EpochKeyRotatingCipher cipher, String data, byte[] aad, String parameter) {
            this.cipher = cipher;
            this.data = data;
            this.aad = aad;
            this.parameter = parameter;
        }
    }
}
