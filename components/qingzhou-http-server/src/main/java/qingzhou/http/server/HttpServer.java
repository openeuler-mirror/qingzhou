package qingzhou.http.server;

/**
 * A handler which is invoked to process HTTP requests.
 */
public interface HttpServer {
    String HTTP_SERVER_PATH = "HTTP_SERVER_PATH";

    void handle(HttpRequest httpRequest, HttpResponse httpResponse);
}
