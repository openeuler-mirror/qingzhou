package qingzhou.registry;

import java.util.Set;

public interface AgentDomainKeys {
    Set<String> getAllAgentDomains();

    byte[] getAgentDomainKey(String agentDomain);

    byte[] toAgentAad(String instanceId, String instanceSecret, String[] reqData);

    byte[] toRegistryAad(String instanceId, String... reqData);

    // 响应方向：此时代中心已持有实例密钥，可绑定；注册请求方向不绑定（首注册时尚无密钥）
    byte[] toRegistryResponseAad(String instanceId, String instanceSecret, String[] reqData);
}
