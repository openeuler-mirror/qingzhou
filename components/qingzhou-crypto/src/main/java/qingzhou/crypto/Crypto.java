package qingzhou.crypto;

import java.security.InvalidKeyException;

public interface Crypto {
    /**
     * 随机生成 24 个字符密钥，供 getCipher 方法使用。
     */
    String generateKey();

    /**
     * 传入24个字符密钥，获得加解密服务对象。
     *
     * @param key 必需24个字符长度。
     * @return 加解密服务对象。
     * @throws InvalidKeyException when 密钥不正确。
     */
    Cipher getCipher(String key) throws InvalidKeyException;

    String[] generatePairKey();

    PairCipher getPairCipher(String publicKey, String privateKey) throws InvalidKeyException;

    TotpCipher getTotpCipher();

    MessageDigest getMessageDigest();

    Base64Coder getBase64Coder();

    Base32Coder getBase32Coder();

    Base16Coder getBase16Coder();
}
