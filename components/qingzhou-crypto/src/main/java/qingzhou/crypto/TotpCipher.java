package qingzhou.crypto;

public interface TotpCipher {
    String generateKey();

    String getCode(String key) throws Exception;

    // 内置容忍前后各 1 个时间窗口
    boolean verifyCode(String key, String code) throws Exception;
}
