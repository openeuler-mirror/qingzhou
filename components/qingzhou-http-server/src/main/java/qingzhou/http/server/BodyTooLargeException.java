package qingzhou.http.server;

/**
 * 请求体超过服务端上限时由 HTTP 层抛出：handler 可据此回 413，而不是笼统的 500。
 * 如：传递到 qingzhou.http.server.HttpHandler.StreamHandler#onError(java.lang.Throwable)
 */
public class BodyTooLargeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BodyTooLargeException() {
        super("request body too large");
    }
}
