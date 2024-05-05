package qingzhou.engine.util.crypto;

public interface KeyCipher {
    String encrypt(String s) throws Exception;

    String decrypt(String s) throws Exception;

    byte[] encrypt(byte[] s) throws Exception;

    byte[] decrypt(byte[] s) throws Exception;
}
