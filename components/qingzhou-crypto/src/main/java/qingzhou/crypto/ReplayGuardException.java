package qingzhou.crypto;

/**
 * 防重放校验失败：报文无效、超出时间窗或重放。
 * <p>
 * 属于请求方可构造的错误，调用方应映射为 4xx 类响应（如 {@link Reason#REPLAY} → 409，
 * 其余 → 400），而非 500。
 */
public final class ReplayGuardException extends RuntimeException {
    public enum Reason {
        INVALID_PACKET,          // 报文格式非法或版本不支持
        TIMESTAMP_OUT_OF_WINDOW, // 超出时间窗
        REPLAY                   // 同一报文重复提交
    }

    private final Reason reason;

    public ReplayGuardException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
