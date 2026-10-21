package qingzhou.agent;

import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainKeys;

@Component(service = LocalAgentDomainKey.class)
public class LocalAgentDomainKey {
    @Reference
    private AgentDomainKeys agentDomainKeys;
    @Reference
    private Crypto crypto;
    @Reference
    private Logger logger;

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
            logger.error("agent domain keys size is not 1");
        }
    }

    public String getLocalAgentDomain() {
        return localAgentDomain;
    }

    public EpochKeyRotatingCipher getLocalAgentCipher() {
        return localAgentCipher;
    }
}
