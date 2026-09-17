package qingzhou.http.client;

public interface ResponseListener {
    void onBody(String line);

    void onComplete();

    /**
     * 请求失败或服务端返回非 2xx 时回调（非 2xx 时响应体可通过 Response.getBody() 获取）。
     */
    void onError(Throwable t);
}
