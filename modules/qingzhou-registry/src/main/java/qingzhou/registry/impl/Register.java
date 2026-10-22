package qingzhou.registry.impl;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.osgi.service.component.annotations.*;
import qingzhou.common.Configs;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.dto.Constants;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainKeys;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=" + Constants.REMOTE_REGISTER_URI, HttpHandler.HANDLE_NO_AUTH + "=true", HttpHandler.HANDLE_METHODS + "=POST"},
        configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Register implements HttpHandler {
    // 注册报文上限：需容纳全部应用的 models/fields/actions/menus/i18ns 元数据，取 1MB 兜底解析前内存消耗
    private static final int MAX_REGISTER_BODY = 1024 * 1024;

    @Reference
    private Logger logger;
    @Reference
    private Crypto crypto;
    @Reference
    private Registry registry;
    @Reference
    private Json json;
    @Reference
    private AgentDomainKeys agentDomainKeys;

    private Timer timer;

    @Activate
    public void start(Map<String, String> config) {
        Configs configs = new Configs(config);
        long interval = 1000L * configs.atLeast("interval", 1, 6);
        long timeout = 1000L * configs.atLeast("timeout", 5, 12);
        timer = new Timer("registry-health-check");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Set<String> toRemove = new HashSet<>();
                for (String next : registry.getAllRemoteInstances()) {
                    InstanceInfo remoteInstance = registry.getRemoteInstance(next);
                    if (remoteInstance != null) {
                        long lastRefreshTime = remoteInstance.getRefreshTime();
                        if (lastRefreshTime + timeout < System.currentTimeMillis()) {
                            toRemove.add(next);
                        }
                    }
                }
                toRemove.forEach(registry::removeRemoteInstance);
            }
        }, interval, interval);
    }

    @Deactivate
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        byte[] requestBody = httpRequest.getBody();
        if (requestBody == null || requestBody.length == 0) {
            httpResponse.status(400);
            return;
        }
        if (requestBody.length > MAX_REGISTER_BODY) {
            httpResponse.status(413);
            return;
        }

        // AAD 绑定，防报文挪域
        String domainId = httpRequest.getParameter(Constants.REMOTE_PARAMETER_DOMAIN_ID);
        String instanceId = httpRequest.getParameter(Constants.REMOTE_PARAMETER_INSTANCE_ID);
        byte[] agentDomainKey;
        if (domainId == null || instanceId == null || (agentDomainKey = agentDomainKeys.getAgentDomainKey(domainId)) == null) {
            logger.warn("illegal agent-domain: " + forLog(domainId));
            httpResponse.status(400); // 统一 400：不回显配置状态差异，防域名枚举
            return;
        }

        try {
            byte[] aad = agentDomainKeys.toRegistryAad(instanceId, Constants.REMOTE_REGISTER_URI);
            EpochKeyRotatingCipher cipher = crypto.getEpochKeyRotatingCipher(agentDomainKey);
            // 解密成功即认证通过，心跳（绝大多数请求）无需再解析报文
            byte[] requestData = cipher.decrypt(requestBody, aad);

            InstanceInfo exists = registry.getRemoteInstance(instanceId);
            if (exists != null) { // 已注册，仅续期
                exists.refreshTime();
                httpResponse.status(304);
                return;
            }

            InstanceInfo instanceInfo = json.fromJson(new String(requestData, StandardCharsets.UTF_8), InstanceInfo.class);
            instanceInfo.setId(instanceId); // 落表键取 AAD 绑定的 instanceId，不采信报文体内自报
            instanceInfo.setHost(httpRequest.getRemoteHost());
            instanceInfo.setDomain(domainId); // 以路由键为准，不信任报文体内自报
            registry.addRemoteInstance(instanceInfo);
            for (AppMeta appMeta : instanceInfo.getAppMetas()) {
                logger.info(String.format("registration, instance: %s, app: %s", instanceId, appMeta.getApp().code));
            }
        } catch (Exception e) {
            logger.error("failed to parse instance info", e);
            httpResponse.status(400);
        }
    }

    // 参数来自不可信请求：截断并剔除控制字符，避免日志注入
    private static String forLog(String value) {
        if (value == null) return "null";
        String text = value.length() > 64 ? value.substring(0, 64) : value;
        return text.replaceAll("\\p{Cntrl}", "?");
    }
}
