package qingzhou.crypto;

public interface AadCipher {
    // 带附加认证数据（AAD）的分段加密
    byte[] encrypt(byte[] data, byte[] context) throws Exception;

    // 带附加认证数据（AAD）解密：aad 必须与加密时一致
    byte[] decrypt(byte[] packet, byte[] context) throws Exception;
}
