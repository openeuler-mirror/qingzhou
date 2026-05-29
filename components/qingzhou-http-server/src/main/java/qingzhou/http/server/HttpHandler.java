package qingzhou.http.server;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

/**
 * A handler which is invoked to process HTTP requests.
 */
public interface HttpHandler {
    String HANDLE_PATH = "HANDLE_PATH";

    void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception;

    default StreamHandler buildStreamHandler() {
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
