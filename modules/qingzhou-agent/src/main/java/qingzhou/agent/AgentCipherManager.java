package qingzhou.agent;

import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.registry.AgentDomainService;

@Component(service = AgentCipherManager.class)
public class AgentCipherManager {
    @Reference
    private AgentDomainService agentDomainService;
    @Reference
    private AgentInstanceManager agentInstanceManager;

    private String agentDomain;
    private EpochKeyRotatingCipher agentCipher;
    // 实例密钥器绑定当前 instanceSecret：应用变更会换新 secret 并重注册，
    // 缓存不随 secret 失效会让续期与 center→agent 报文全部解密失败（实例周期性抖动）
    private volatile InstanceCipher instanceCipher;

    @Activate
    public void init() {
        Set<String> allAgentDomains = agentDomainService.getAllAgentDomains();
        if (allAgentDomains.size() == 1) {
            agentDomain = allAgentDomains.iterator().next();
        } else {
            // 密钥数量异常时 agent 的所有端点都会 503、心跳也无法加密：必须 fail-fast 并指明配置文件
            throw new IllegalStateException("agent domain keys must be exactly 1, but was " + allAgentDomains.size()
                    + "; check agent-domain~* entries in conf/secret-key.properties");
        }
    }

    String getAgentDomain() {
        return agentDomain;
    }

    EpochKeyRotatingCipher getAgentCipher() {
        if (agentCipher == null) {
            agentCipher = agentDomainService.getDomainCipher(agentDomain);
        }
        return agentCipher;
    }

    // 每实例密钥：由域密钥 + 实例密钥派生，仅 center 与该实例可计算，同域其他 agent 无法解密其注册后报文
    EpochKeyRotatingCipher getInstanceCipher() {
        InstanceInfo cachedInstance = agentInstanceManager.getCachedInstance();
        if (cachedInstance == null) return null;
        String secret = cachedInstance.getInstanceSecret();

        InstanceCipher cached = instanceCipher;
        if (cached == null || !cached.secret.equals(secret)) {
            // 不可变槽整体经 volatile 发布：避免 secret 与密码器被并发读到错配组合
            cached = new InstanceCipher(secret, agentDomainService.getInstanceCipher(agentDomain, secret));
            instanceCipher = cached;
        }
        return cached.cipher;
    }

    private static final class InstanceCipher {
        final String secret;
        final EpochKeyRotatingCipher cipher;

        InstanceCipher(String secret, EpochKeyRotatingCipher cipher) {
            this.secret = secret;
            this.cipher = cipher;
        }
    }
}
