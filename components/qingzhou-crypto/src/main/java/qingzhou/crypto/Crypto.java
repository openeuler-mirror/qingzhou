package qingzhou.crypto;

import java.security.InvalidKeyException;

public interface Crypto {
    /**
     * 随机生成 24 个字符密钥，供 getCipher 方法使用。
     * 密钥由 SecureRandom 直接产生、不建立在主密钥之上：KDF 不产生熵，派生只会把风险集中到主密钥；
     * 多节点共享同一密钥由 getGlobalCipher 的密钥文件分发实现。
     */
    String generateCipherKey();

    /**
     * 将多个上下文串分帧为 AAD：每段以「长度:内容」编码（null 记为 -1），字段边界无歧义，
     * 避免 ["a,b"] 与 ["a","b"] 拼出同一 AAD。
     */
    byte[] generateCipherAad(String... context);

    /**
     * 传入24个字符密钥，获得加解密服务对象。
     *
     * @param key 必需24个字符长度。
     * @return 加解密服务对象。
     * @throws InvalidKeyException when 密钥不正确。
     */
    Cipher getCipher(String key) throws InvalidKeyException;

    Cipher getCipher(byte[] key) throws InvalidKeyException;

    Cipher getGlobalCipher();

    MessageDigest getMessageDigest();

    Base64Coder getBase64Coder();

    Base32Coder getBase32Coder();

    Base16Coder getBase16Coder();

    Totp getTotp();

    /**
     * 请求报文的防重放封装。返回的实例在同一 JVM 内共享，去重状态唯一。
     */
    ReplayGuardCipher getReplayGuardCipher(Cipher cipher);

    /**
     * 绑定根密钥与作用域的时段报文加解密器。与 {@link #getReplayGuardCipher} 一致，每次返回独立实例。
     */
    EpochKeyRotatingCipher getEpochKeyRotatingCipher(byte[] masterKey);
}
