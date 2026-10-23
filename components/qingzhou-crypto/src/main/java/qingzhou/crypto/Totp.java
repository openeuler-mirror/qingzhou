package qingzhou.crypto;

public interface Totp {
    String generateKey();

    String getCode(String key) throws Exception;

    // 内置容忍前后各 1 个时间窗口
    boolean verifyCode(String key, String code) throws Exception;

    // 命中的时间步；-1 表示不匹配。调用方据此做一次性消费，避免同一窗口内重放
    long verifyCodeStep(String key, String code) throws Exception;
}
