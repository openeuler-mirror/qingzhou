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
     * 本方法只负责「开始」：响应必须由 handler 自己以终态方法收尾（{@link HttpResponse#finish()}、
     * {@link HttpResponse#redirect(String)}）。未收尾的请求会一直挂到空闲超时，属 handler 的编程错误。
     * 允许 handler 返回后再由其它线程继续写入并收尾（如 SSE），框架不会代为结束。
     * 仅当本方法抛出异常时，框架才会自行终止响应。
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
