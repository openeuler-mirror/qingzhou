package qingzhou.crypto;

public interface Totp {
    String generateKey();

    String getCode(String key) throws Exception;

    // 校验并消费：按 key 记录已通过的最大窗口序号，同一（或更早）窗口的口令只能使用一次（防重放）。
    // 状态存于内存，进程重启后清零
    boolean verifyCodeWithReplayGuard(String key, String code) throws Exception;
}
