package qingzhou.crypto;

public interface Cipher {
    String encrypt(String s) throws Exception;

    // 尝试解密，如果失败则返回原值
    String tryDecrypt(String s, String configName);

    String decrypt(String s) throws Exception;

    byte[] encrypt(byte[] s) throws Exception;

    byte[] encrypt(byte[] s, int off, int len) throws Exception;

    byte[] decrypt(byte[] s) throws Exception;
}
