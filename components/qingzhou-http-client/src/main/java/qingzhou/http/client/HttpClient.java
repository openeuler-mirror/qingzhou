package qingzhou.http.client;

public interface HttpClient {
    Response send(Request request) throws Exception;

    /**
     * 2xx 响应通过 listener 流式回调 onBody/onComplete；非 2xx 不触发任何回调，响应体通过 getBody() 获取，由调用方根据 getStatus() 处理。
     */
    Response send(Request request, ResponseListener listener) throws Exception;

    Request buildRequest(String url);
}
