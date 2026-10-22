package qingzhou.registry;

import java.util.Set;

public interface AgentDomainKeys {
    Set<String> getAllAgentDomains();

    byte[] getAgentDomainKey(String agentDomain);

    byte[] toAgentAad(String instanceId, String instanceSecret, String[] reqData);

    byte[] toRegistryAad(String instanceId, String... reqData);
}
