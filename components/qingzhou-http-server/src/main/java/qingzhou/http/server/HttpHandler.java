package qingzhou.http.server;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

/**
 * A handler which is invoked to process HTTP requests.
 */
public interface HttpHandler {
    String HANDLE_PATH = "HANDLE_PATH";
    String HANDLE_NO_AUTH = "HANDLE_NO_AUTH";

    void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception;

    /**
     * 优先级高于系统级 Authenticator。
     * 返回 {@link AuthResult#abstain()} 或 null 表示弃权，转交系统级认证器；
     * 返回 {@link AuthResult#reject} 会直接以 401 结束请求，系统级认证器不再有机会判定。
     * 抛出的异常一律按拒绝处理。
     */
    default Authenticator customAuthenticator() {
        return null;
    }

    default StreamHandler multipartStreamHandler() {
        return null;
    }

    interface StreamHandler {

        void onBegin(HttpRequest request, HttpResponse response);

        /**
         * Data notification sent by the {@link Publisher} in response to requests to {@link Subscription#request(long)}.
         *
         * @param data the element signaled
         */
        void onNext(byte[] data);

        /**
         * Failed terminal state.
         * <p>
         * No further events will be sent even if {@link Subscription#request(long)} is invoked again.
         *
         * @param t the throwable signaled
         */
        void onError(Throwable t);

        /**
         * Successful terminal state.
         * <p>
         * No further events will be sent even if {@link Subscription#request(long)} is invoked again.
         */
        void onComplete();
    }
}
