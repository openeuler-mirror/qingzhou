package qingzhou.remote.net.http;

@FunctionalInterface
public interface HttpHandler {
    void handle(HttpRequest request, HttpResponse response) throws Exception;
}