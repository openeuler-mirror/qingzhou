package qingzhou.crypto;

public interface Cipher {
    String encrypt(String s) throws Exception;

    String decrypt(String s) throws Exception;

    byte[] encrypt(byte[] s) throws Exception;

    byte[] decrypt(byte[] s) throws Exception;
}
