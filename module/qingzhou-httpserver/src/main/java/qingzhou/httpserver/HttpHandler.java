package qingzhou.httpserver;

public interface HttpHandler {
    void handle(HttpExchange httpExchange);
}
