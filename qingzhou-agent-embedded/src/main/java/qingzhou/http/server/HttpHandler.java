package qingzhou.http.server;

public interface HttpHandler {
    String HANDLE_PATH = "HANDLE_PATH";

    void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception;
}