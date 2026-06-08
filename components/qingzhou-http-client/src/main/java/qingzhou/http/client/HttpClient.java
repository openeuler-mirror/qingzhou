package qingzhou.http.client;

public interface HttpClient {
    Response send(Request request) throws Exception;

    Request newRequest(String url);
}
