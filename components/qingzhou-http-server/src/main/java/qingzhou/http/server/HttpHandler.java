package qingzhou.http.server;

/**
 * A handler which is invoked to process HTTP requests.
 */
public interface HttpHandler {
    String HANDLE_PATH = "HANDLE_PATH";
    String HANDLE_PATH_ABSOLUTE = "HANDLE_PATH_ABSOLUTE";
    String HANDLE_NO_AUTH = "HANDLE_NO_AUTH";
    // 该路径允许的 HTTP 方法（逗号分隔，如 POST）；缺省表示允许全部方法
    String HANDLE_METHODS = "HANDLE_METHODS";

    /**
     * 处理请求。
     * <p>
     * 实现须为非阻塞：本方法运行于网络 IO 线程，阻塞（同步 IO、慢计算、睡眠）会拖垮
     * 同一 IO 线程上的全部连接；耗时处理应转交业务线程，并经 {@link HttpResponse#async()}
     * 声明后由业务线程写响应。
     * <p>
     * 正常返回后框架自动收尾响应，无需调用 {@link HttpResponse#finish()}。
     * 若要在返回后继续写入并收尾（如 SSE），须在返回前调用 {@link HttpResponse#async()}，
     * 之后由 handler 自行 {@link HttpResponse#finish()}；否则后台写入会因响应已结束而失败。
     * 未收尾的异步响应会挂到空闲超时，属 handler 的编程错误。
     * 仅当本方法抛出异常时，框架会强制终止响应。
     */
    void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception;

    default StreamHandler multipartStreamHandler() {
        return null;
    }

    interface StreamHandler {
        void onBegin(HttpRequest request, HttpResponse response) throws Throwable;

        void onNext(byte[] data) throws Throwable;

        void onError(Throwable t);

        void onComplete();
    }
}
