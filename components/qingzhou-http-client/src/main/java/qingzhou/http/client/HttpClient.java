package qingzhou.http.client;

public interface HttpClient {
    Response send(Request request) throws Exception;

    Response send(Request request, ResponseListener listener) throws Exception;

    Request newRequest(String url);
}
