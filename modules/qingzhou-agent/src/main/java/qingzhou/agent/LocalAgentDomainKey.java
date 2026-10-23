package qingzhou.agent;

import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.registry.AgentDomainKeys;

@Component(service = LocalAgentDomainKey.class)
public class LocalAgentDomainKey {
    @Reference
    private AgentDomainKeys agentDomainKeys;

    private String localAgentDomain;

    @Activate
    public void init() {
        Set<String> allAgentDomains = agentDomainKeys.getAllAgentDomains();
        if (allAgentDomains.size() == 1) {
            localAgentDomain = allAgentDomains.iterator().next();
        } else {
            // 密钥数量异常时 agent 的所有端点都会 503、心跳也无法加密：必须 fail-fast 并指明配置文件
            throw new IllegalStateException("agent domain keys must be exactly 1, but was " + allAgentDomains.size()
                    + "; check agent-domain~* entries in conf/secret-key.properties");
        }
    }

    public String getLocalAgentDomain() {
        return localAgentDomain;
    }

    public EpochKeyRotatingCipher getLocalAgentCipher() {
        return agentDomainKeys.getDomainCipher(localAgentDomain);
    }

    // 每实例密钥：由域密钥 + 实例密钥派生，仅 center 与该实例可计算，同域其他 agent 无法解密其注册后报文
    public EpochKeyRotatingCipher getInstanceCipher(String instanceSecret) {
        return agentDomainKeys.getInstanceCipher(localAgentDomain, instanceSecret);
    }
}
