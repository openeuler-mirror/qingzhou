package qingzhou.chat;

/**
 * 流式回调接口 —— 与 agents-flex 解耦
 * <p>
 * LlmService 调用 agents-flex 时将内部事件映射到此接口，
 * ChatServlet 只依赖此接口，不直接依赖 agents-flex 的类型。
 */
public interface StreamCallback {

    default void onStart() {
    }

    default void onReason(String reason) {

    }

    /**
     * 收到一个文本片段（增量 token）
     * @param token 当前输出的文本片段
     */
    void onToken(String token);

    /**
     * 流式输出完成
     */
    void onComplete();

    void onFinished();

    /**
     * 发生错误
     * @param error 异常
     */
    void onError(Throwable error);
}