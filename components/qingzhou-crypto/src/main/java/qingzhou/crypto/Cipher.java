package qingzhou.crypto;

public interface Cipher {
    String PLAIN_PREFIX_MARKER = "plain:";
    int IV_SIZE = 12;

    String encrypt(String s) throws Exception;

    String tryDecrypt(String s, String configName);

    String decrypt(String s) throws Exception;

    byte[] encrypt(byte[] s) throws Exception;

    byte[] encrypt(byte[] s, int off, int len) throws Exception;

    byte[] decrypt(byte[] s) throws Exception;
}
