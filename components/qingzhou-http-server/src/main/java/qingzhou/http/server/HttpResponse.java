package qingzhou.http.server;

public interface HttpResponse {
    HttpResponse status(int status);

    HttpResponse header(String name, String value);

    HttpResponse contentType(String value);

    HttpResponse contentTypeJsonUtf8();

    HttpResponse send(String bodyAsUtf8);

    /**
     * 写入响应体分片。响应缓冲有界（慢客户端保护）：缓冲满时抛出 IllegalStateException，
     * 调用方不得假定 send 总能成功；异步长响应（如 SSE）应处理该异常，避免慢客户端
     * 把分片写入失败升级为未兜底的崩溃。
     */
    HttpResponse send(byte[] body);

    /**
     * 声明响应由调用方异步收尾：handler 返回后框架不再自动 {@link #finish()}，
     * 调用方写完响应后必须自行调用 {@link #finish()}。
     * 须在派发任何后台写线程之前调用，否则后台写入可能在自动收尾后失败。
     */
    HttpResponse async();

    /**
     * 结束响应：正常返回时框架会自动调用；异步模式（{@link #async()}）下由调用方负责。
     * 重复调用无副作用，之后不可再 {@link #send(byte[])}。
     */
    void finish();

    /**
     * 客户端是否已断开连接（订阅方取消）。长耗时任务据此主动终止，避免空转。
     */
    default boolean isCancelled() {
        return false;
    }

    void redirect(String url);
}
