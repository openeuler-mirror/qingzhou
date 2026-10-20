package qingzhou.registry.impl;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.osgi.service.component.annotations.*;
import qingzhou.common.Configs;
import qingzhou.crypto.Crypto;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=/register", HttpHandler.HANDLE_NO_AUTH + "=true", HttpHandler.HANDLE_METHODS + "=POST"},
        configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Register implements HttpHandler {
    private static final int MAX_REGISTER_BODY = 16 * 1024; // 注册报文上限：AES-GCM 解密廉价，仅防解析前内存消耗

    @Reference
    private Logger logger;
    @Reference
    private Crypto crypto;
    @Reference
    private Registry registry;
    @Reference
    private Json json;
    @Reference
    private AgentDomainService agentDomainService;

    private Timer timer;

    @Activate
    public void start(Map<String, String> config) throws Exception {
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
                        long lastRefreshTime = remoteInstance.getLastRefreshTime();
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

        // 报文 = domainId(明文首行，路由键) + '\n' + ReplayGuard 报文；domainId 同时作 AAD 绑定，防报文挪域
        int sep = indexOfLineBreak(requestBody);
        if (sep < 0) {
            httpResponse.status(400);
            return;
        }
        String domainId = new String(requestBody, 0, sep, StandardCharsets.UTF_8).trim();
        byte[] packet = Arrays.copyOfRange(requestBody, sep + 1, requestBody.length);

        byte[] master = agentDomainService.master(domainId);
        if (master == null) {
            logger.warn("registration for unconfigured agent-domain: " + domainId);
            httpResponse.status(400); // 统一 400：不回显配置状态差异，防域名枚举
            return;
        }

        byte[] requestData;
        try {
            requestData = crypto.getEpochPacketCipher(master, domainId).decrypt(packet,
                    domainId.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 报文由请求方控制：格式、时间窗、重放与密钥失配一律 400，不回显配置状态差异
            logger.warn("registration decrypt failed for domain " + domainId + ": " + e.getMessage());
            httpResponse.status(400);
            return;
        }

        InstanceInfo instanceInfo;
        try {
            instanceInfo = json.fromJson(new String(requestData, StandardCharsets.UTF_8), InstanceInfo.class);
            instanceInfo.setHost(httpRequest.getRemoteHost());
            instanceInfo.setDomain(domainId); // 以路由键为准，不信任报文体内自报
        } catch (Exception e) {
            httpResponse.status(400);
            return;
        }

        // 应用变更会重算实例 ID：同一 agent（域 + host + port 相同）的旧记录先移除，避免同一实例双份存在
        for (String other : registry.getAllRemoteInstances()) {
            if (other.equals(instanceInfo.getId())) continue;
            InstanceInfo info = registry.getRemoteInstance(other);
            if (info != null && domainId.equals(info.getDomain())
                    && Objects.equals(instanceInfo.getHost(), info.getHost())
                    && instanceInfo.getPort() == info.getPort()) {
                registry.removeRemoteInstance(other);
            }
        }

        InstanceInfo exists = registry.getRemoteInstance(instanceInfo.getId());
        registry.addRemoteInstance(instanceInfo);

        String msg;
        if (exists != null) {
            msg = "Redundant.";
        } else {
            List<AppMeta> appMetas = instanceInfo.getAppMetas();
            msg = appMetas.size() + " apps accepted.";
            for (AppMeta appMeta : appMetas) {
                logger.info(String.format("registration, instance: %s, app: %s", instanceInfo.getId(), appMeta.getApp().code));
            }
        }

        // 响应同样按时段封装：agent 按报文时间戳派生密钥即可解开
        try {
            byte[] encrypt = crypto.getEpochPacketCipher(master, domainId)
                    .encrypt(msg.getBytes(StandardCharsets.UTF_8), domainId.getBytes(StandardCharsets.UTF_8));
            httpResponse.send(encrypt);
        } catch (Exception e) {
            httpResponse.status(500);
            logger.error("failed to encrypt registration response", e);
        }
    }

    // domainId 明文首行以 '\n' 结尾；限制首行扫描长度
    private static int indexOfLineBreak(byte[] body) {
        int limit = Math.min(body.length, 256);
        for (int i = 0; i < limit; i++) {
            if (body[i] == '\n') return i;
        }
        return -1;
    }
}
