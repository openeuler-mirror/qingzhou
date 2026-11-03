package qingzhou.crypto;

/**
 * 带附加认证数据（AAD）的认证加密：{@code context} 只参与认证、不出现在密文中。
 * <p>
 * AAD 把密文绑定到其语义上下文（端点、域、实例、请求/响应方向、明文控制参数等）：同一密文换到
 * 另一个上下文即认证失败，用于阻断「合法密文被挪到别处使用」。解密时 {@code context} 须与加密时一致。
 * <p>
 * 报文加解密路径由 {@link ReplayGuardCipher} 在业务上下文之外再拼入 timestamp 与 nonce，
 * 使时效与去重同样落在 GCM 认证范围内。
 */
public interface AadCipher {
    // context 作 AAD 参与认证，不进入密文
    byte[] encrypt(byte[] data, byte[] context) throws Exception;

    // packet 为加密输出，context 须与加密时一致
    byte[] decrypt(byte[] packet, byte[] context) throws Exception;
}
