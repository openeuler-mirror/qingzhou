package qingzhou.crypto;

public interface Cipher {
    String PLAIN_PREFIX_MARKER = "plain:";
    int IV_SIZE = 12;

    String encrypt(String s) throws Exception;

    String tryDecrypt(String s, String configName);

    String decrypt(String s) throws Exception;

    byte[] encrypt(byte[] s) throws Exception;

    byte[] encrypt(byte[] s, int off, int len) throws Exception;

    /**
     * 带附加认证数据（AAD）的分段加密：用于流式上传，把分片序号等上下文绑定到认证标签。
     */
    byte[] encrypt(byte[] s, int off, int len, byte[] aad) throws Exception;

    byte[] decrypt(byte[] s) throws Exception;

    /**
     * 带附加认证数据（AAD）解密：aad 必须与加密时一致，否则认证失败。
     */
    byte[] decrypt(byte[] s, byte[] aad) throws Exception;
}
