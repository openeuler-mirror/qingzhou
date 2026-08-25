package qingzhou.llm;

public interface ChatModel {
    // 发起一次同步对话。
    String chat(String message, Attachment... attachment);
    
    /**
     * 发起一次流式对话。
     * <p>
     * 本方法为异步语义：onBegin 在调用线程同步触发，其余回调（onMessage/onReasoning/onToolCall/onComplete/onError）
     * 均在后台线程异步输出，且调用线程在方法返回时流式输出可能仍在进行。
     * <p>
     * 调用方不得在方法返回后依赖请求线程上下文（如 ThreadLocal、请求线程相关资源）来驱动或终结回调，
     * 也不应在方法返回时即销毁回调所依赖的输出通道，否则后台回调可能失效。
     */
    void chat(String message, Listener listener, Attachment... attachment);
}
