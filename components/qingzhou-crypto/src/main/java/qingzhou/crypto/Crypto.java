package qingzhou.crypto;

import java.security.InvalidKeyException;

public interface Crypto {
    /**
     * 随机生成 32 字节（256 位）安全密钥，Base64 编码。
     * 密钥由 SecureRandom 直接产生、不建立在主密钥之上：KDF 不产生熵，派生只会把风险集中到主密钥；
     * 用于配置加解密（getCipher）与域信任根（HKDF 的 IKM），多节点共享由密钥文件分发实现。
     */
    String generateSecureKey();

    /**
     * 将多个上下文串分帧为 AAD：每段以「长度:内容」编码（null 记为 -1），字段边界无歧义，
     * 避免 ["a,b"] 与 ["a","b"] 拼出同一 AAD。
     */
    byte[] generateCipherAad(String... context);

    /**
     * 传入 Base64 编码的 AES-256 密钥，获得加解密服务对象。
     *
     * @param key 必需解码为 32 字节。
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

    /**
     * 从高熵根密钥经 HKDF 单向派生子密钥：{@code info} 区分作用域（时段、实例、用途等），
     * 不同 info 的派生结果互相独立。根密钥须为高熵随机值（不可为口令）。
     */
    byte[] deriveKey(byte[] masterKey, byte[] info, int length);
}
