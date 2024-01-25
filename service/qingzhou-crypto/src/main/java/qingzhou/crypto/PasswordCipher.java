package qingzhou.crypto;

public interface PasswordCipher {
    String encrypt(String s) throws Exception;

    String decrypt(String s) throws Exception;

    byte[] encrypt(byte[] s) throws Exception;

    byte[] decrypt(byte[] s) throws Exception;
}
