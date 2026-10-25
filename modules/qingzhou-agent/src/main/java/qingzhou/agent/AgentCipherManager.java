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
    private EpochKeyRotatingCipher instanceCipher;

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
        if (instanceCipher == null) {
            InstanceInfo cachedInstance = agentInstanceManager.getCachedInstance();
            if (cachedInstance != null) {
                instanceCipher = agentDomainService.getInstanceCipher(agentDomain, cachedInstance.getInstanceSecret());
            }
        }
        return instanceCipher;
    }
}
