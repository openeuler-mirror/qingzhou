package qingzhou.registry.impl;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.osgi.service.component.annotations.*;
import qingzhou.common.Configs;
import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainKeys;
import qingzhou.registry.Constants;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=" + qingzhou.registry.Constants.REMOTE_REGISTER_URI, HttpHandler.HANDLE_NO_AUTH + "=true", HttpHandler.HANDLE_METHODS + "=POST"},
        configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Register implements HttpHandler {
    // 注册报文上限：需容纳全部应用的 models/fields/actions/menus/i18ns 元数据，取 1MB 兜底解析前内存消耗
    private static final int MAX_REGISTER_BODY = 1024 * 1024;
    // 实例标识同时用作注册表键、AAD 上下文与日志字段：限定安全字符集与长度
    private static final int MAX_INSTANCE_ID_CHARS = 128;
    // 注册表容量兜底：持有域密钥即可注册任意多伪实例，而单实例元数据最大 1MB
    private static final int MAX_REMOTE_INSTANCES = 1000;

    @Reference
    private Logger logger;
    @Reference
    private Registry registry;
    @Reference
    private Json json;
    @Reference
    private AgentDomainKeys agentDomainKeys;

    private Timer timer;
    // 是否采信 agent 自报地址：仅反代/NAT 部署需要，默认关闭以免域内成员把流量导向任意地址
    private boolean trustAdvertisedHost;

    @Activate
    public void start(Map<String, String> config) {
        Configs configs = new Configs(config);
        long interval = 1000L * configs.atLeast("interval", 1, 6);
        long timeout = 1000L * configs.atLeast("timeout", 5, 12);
        trustAdvertisedHost = configs.getOrDefault("trust_advertised_host", false);
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
        String domainId = httpRequest.getParameter(qingzhou.registry.Constants.REMOTE_PARAMETER_DOMAIN_ID);
        String instanceId = httpRequest.getParameter(Constants.REMOTE_PARAMETER_INSTANCE_ID);
        if (domainId == null || instanceId == null || !agentDomainKeys.getAllAgentDomains().contains(domainId)) {
            logger.warn("illegal agent-domain: " + forLog(domainId));
            httpResponse.status(400); // 统一 400：不回显配置状态差异，防域名枚举
            return;
        }
        if (!isSafeToken(instanceId)) {
            logger.warn("illegal instance id: " + forLog(instanceId));
            httpResponse.status(400);
            return;
        }

        try {
            byte[] aad = agentDomainKeys.toRegistryAad(instanceId, qingzhou.registry.Constants.REMOTE_REGISTER_URI);
            // 已注册实例的续期用每实例密钥解密：仅真实实例可刷新自身，同域其他 agent 无法冒名续期；
            // 新实例首注册用域密钥解密（首注册时中心尚无该实例密钥，必须靠域密钥才能取得它）
            InstanceInfo exists = registry.getRemoteInstance(instanceId);
            EpochKeyRotatingCipher cipher = exists == null
                    ? agentDomainKeys.getDomainCipher(domainId)
                    : agentDomainKeys.getInstanceCipher(domainId, exists.getInstanceSecret());
            byte[] requestData = cipher.decrypt(requestBody, aad);

            // 已存在只续期、不覆盖：即使同域成员冒名续期也已被上面的每实例密钥解密拦截
            if (exists != null) {
                exists.refreshTime();
                httpResponse.status(304);
                return;
            }

            if (registry.getAllRemoteInstances().size() >= MAX_REMOTE_INSTANCES) {
                logger.warn("remote instance quota exceeded: " + MAX_REMOTE_INSTANCES);
                httpResponse.status(503); // 容量不足：仅拒新实例，已注册实例仍可续期
                return;
            }

            InstanceInfo instanceInfo = json.fromJson(new String(requestData, StandardCharsets.UTF_8), InstanceInfo.class);
            if (instanceInfo.getPort() < 1 || instanceInfo.getPort() > 65535) {
                logger.warn("illegal agent port: " + instanceInfo.getPort());
                httpResponse.status(400);
                return;
            }
            // 实例密钥是 center→agent 方向的认证因子：缺失即无法安全下发指令，直接拒绝注册
            if (!isSafeToken(instanceInfo.getInstanceSecret())) {
                logger.warn("missing or illegal instance secret");
                httpResponse.status(400);
                return;
            }
            // 落表键必须与上面的查询键同为 AAD 绑定值：否则可用新 id 注册、体内自报他人 id 绕过 exists 检查
            instanceInfo.setId(instanceId);
            instanceInfo.setHost(resolveHost(instanceInfo, httpRequest));
            instanceInfo.setDomain(domainId); // 由密钥选择确定
            registry.addRemoteInstance(instanceInfo);
            for (AppMeta appMeta : instanceInfo.getAppMetas()) {
                logger.info(String.format("registration, instance: %s, app: %s", instanceId, forLog(appMeta.getApp().code)));
            }
        } catch (Exception e) {
            logger.error("failed to parse instance info", e);
            httpResponse.status(400);
        }
    }

    // 反代/NAT 下 TCP 对端恒为代理地址：开启信任时改用 agent 自报地址
    private String resolveHost(InstanceInfo instanceInfo, HttpRequest httpRequest) {
        String remoteHost = httpRequest.getRemoteHost(); // 默认取 TCP 源地址，不采信报文自报
        String advertised = instanceInfo.getAdvertisedHost();
        if (!trustAdvertisedHost || advertised == null) return remoteHost;

        String host = advertised.trim();
        if (isSafeHost(host)) return host;

        logger.warn("illegal advertised host ignored: " + forLog(advertised));
        return remoteHost;
    }

    // 该值会拼进下游 URL：只放行常规主机名/IPv4 字符，杜绝 user@host、路径与查询串注入（IPv6 请用主机名）
    private static boolean isSafeHost(String host) {
        if (host.isEmpty() || host.length() > 255) return false;
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            boolean safe = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '.' || c == '-';
            if (!safe) return false;
        }
        return true;
    }

    private static boolean isSafeToken(String token) {
        if (token == null || token.isEmpty() || token.length() > MAX_INSTANCE_ID_CHARS) return false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            boolean safe = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.';
            if (!safe) return false;
        }
        return true;
    }

    // 参数来自不可信请求：截断并剔除控制字符，避免日志注入
    private static String forLog(String value) {
        if (value == null) return "null";
        String text = value.length() > 64 ? value.substring(0, 64) : value;
        return text.replaceAll("\\p{Cntrl}", "?");
    }
}
