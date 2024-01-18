package qingzhou.remote.impl.net;

@FunctionalInterface
public interface HttpHandler {
    void handle(HttpRequest request, HttpResponse response) throws Exception;
}