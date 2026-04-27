package qingzhou.http.server;

/**
 * A handler which is invoked to process HTTP requests.
 */
public interface HttpHandler {
    String HANDLE_PATH = "HANDLE_PATH";

    void handle(HttpRequest httpRequest, HttpResponse httpResponse);
}
