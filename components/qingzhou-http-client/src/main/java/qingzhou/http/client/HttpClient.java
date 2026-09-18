package qingzhou.http.client;

public interface HttpClient {
    Response send(Request request) throws Exception;

    /**
     * 2xx 响应通过 listener 流式回调 onBody/onComplete；非 2xx 响应回调 onError，响应体可通过 getBody() 获取。
     */
    Response send(Request request, ResponseListener listener) throws Exception;

    Request newRequest(String url);
}
