package qingzhou.crypto;

public interface TotpCipher {
    String generateKey();

    String getCode(String key) throws Exception;

    boolean verifyCode(String key, String code) throws Exception;
}
