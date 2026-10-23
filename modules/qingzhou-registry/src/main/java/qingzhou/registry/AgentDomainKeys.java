package qingzhou.registry;

import java.util.Set;

import qingzhou.crypto.EpochKeyRotatingCipher;

/**
 * agent↔registry 双向报文的安全边界（信任模型）：
 * <p>
 * 两级密钥：domain key（域密钥，预共享）承担域成员认证与引导期机密性；instanceSecret（每实例密钥，
 * agent 生成、仅首次注册上报一次）经 {@link #getInstanceCipher(String, String)} 派生每实例加密密钥，
 * 用于注册后的双向报文。未捕获该实例注册报文的同域成员推不出每实例密钥，无法解密/篡改其注册后报文。
 * <p>
 * 方向隔离：center→agent 用 "REGISTRY_AGENT"、agent→center 用 "AGENT_REGISTRY" 前缀，防方向混淆。
 * <p>
 * 残余边界：首次注册报文仍用 domain key 加密，捕获到它的同域成员可窃取 instanceSecret 并派生每实例密钥。
 * 因此本方案满足「无密钥的外部客户端不可读/改」，但「同域实例互不可读/改」仅对未捕获引导报文的成员成立，
 * 非彻底满足——彻底隔离需引入非对称（中心密钥对，令注册只对中心可见）。
 */
public interface AgentDomainKeys {
    Set<String> getAllAgentDomains();

    // 域密钥时段密码器：用于首注册（引导）报文的加解密
    EpochKeyRotatingCipher getDomainCipher(String agentDomain);

    // 每实例密钥时段密码器：用于注册后双向报文的加解密
    EpochKeyRotatingCipher getInstanceCipher(String agentDomain, String instanceSecret);

    byte[] toAgentAad(String instanceId, String instanceSecret, String[] reqData);

    byte[] toRegistryAad(String instanceId, String... reqData);

    // 响应方向：此时代中心已持有实例密钥，可绑定；注册请求方向不绑定（首注册时尚无密钥）
    byte[] toRegistryResponseAad(String instanceId, String instanceSecret, String[] reqData);
}
