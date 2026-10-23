package qingzhou.agent;

import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.registry.AgentDomainKeys;

@Component(service = LocalAgentDomainKey.class)
public class LocalAgentDomainKey {
    @Reference
    private AgentDomainKeys agentDomainKeys;
    @Reference
    private Crypto crypto;

    private String localAgentDomain;
    private EpochKeyRotatingCipher localAgentCipher;

    @Activate
    public void init() {
        Set<String> allAgentDomains = agentDomainKeys.getAllAgentDomains();
        if (allAgentDomains.size() == 1) {
            localAgentDomain = allAgentDomains.iterator().next();
            byte[] localAgentDomainKey = agentDomainKeys.getAgentDomainKey(localAgentDomain);
            localAgentCipher = crypto.getEpochKeyRotatingCipher(localAgentDomainKey);
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
        return localAgentCipher;
    }
}
