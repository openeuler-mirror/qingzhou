package qingzhou.registry;

import java.util.Set;

import qingzhou.crypto.EpochKeyRotatingCipher;

/**
 * agent↔registry 双向报文的安全边界（信任模型）：
 * <p>
 * 两级密钥：domain key（域密钥，预共享）承担域成员认证与引导期机密性；instanceSecret（每实例密钥，
 * agent 生成、仅首次注册上报一次）经 {@link #getInstanceCipher(String, String)} 派生每实例加密密钥，
 * 仅 center 与该实例可计算，用于注册后的双向报文。未捕获该实例注册报文的同域成员推不出每实例密钥，
 * 无法解密/篡改其注册后报文。
 * <p>
 * 方向隔离：注册（agent→center）与执行/上传（center→agent）各用独立 AAD 命名空间；响应由
 * {@link #responseAad} 以 "isResponse" 标记并嵌入请求的业务上下文，使请求/响应及两个方向互不通用。
 * <p>
 * 上下文绑定（AAD，只认证不加密）：
 * <ul>
 *   <li>{@link #aadForRegistry}：绑注册端点 + agentDomain + instanceId（首注册 instanceId 为空），
 *       注册报文不能挪到其他域、实例或端点。</li>
 *   <li>{@link #aadForAgent}：绑端点 + instanceId + 明文控制参数（如上传的分片键与偏移），
 *       执行/上传报文不能改端点、冒用实例或篡改控制参数。该方向 AAD 不含 agentDomain，跨域保护由
 *       每实例密钥与 instanceId 承担：即使两域误配同一域密钥，instanceSecret 不同则实例密钥仍不同。</li>
 * </ul>
 * 各字段经长度前缀分帧，杜绝不同字段组合拼接出同一 AAD；报文路径上 {@link qingzhou.crypto.ReplayGuardCipher}
 * 再把 timestamp 与 nonce 纳入 AAD，时效与去重同样受完整性保护。
 * <p>
 * 残余边界：首次注册报文仍用 domain key 加密，捕获到它的同域成员可窃取 instanceSecret 并派生每实例密钥。
 * 因此本方案满足「无密钥的外部客户端不可读/改」，但「同域实例互不可读/改」仅对未捕获引导报文的成员成立，
 * 非彻底满足——彻底隔离需引入非对称（中心密钥对，令注册只对中心可见）。
 */
public interface AgentDomainService {
    Set<String> getAllAgentDomains();

    // 域密钥时段密码器：用于首注册（引导）报文的加解密
    EpochKeyRotatingCipher getDomainCipher(String agentDomain);

    // 每实例密钥时段密码器：用于注册后双向报文的加解密
    EpochKeyRotatingCipher getInstanceCipher(String agentDomain, String instanceSecret);

    byte[] aadForRegistry(String agentDomain, String instanceId);

    byte[] aadForAgent(String instanceId, String uri, String... extAad);

    byte[] responseAad(byte[] requestAad);
}
