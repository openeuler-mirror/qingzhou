package qingzhou.http.server;

/**
 * A handler which is invoked to process HTTP requests.
 */
public interface HttpHandler {
    String HANDLE_PATH = "HANDLE_PATH";
    String HANDLE_PATH_ABSOLUTE = "HANDLE_PATH_ABSOLUTE";
    String HANDLE_NO_AUTH = "HANDLE_NO_AUTH";

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
