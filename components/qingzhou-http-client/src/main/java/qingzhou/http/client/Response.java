package qingzhou.http.client;

public interface Response {
    int getStatus();

    byte[] getBody();

    /**
     * 中止流式读取：断开底层连接，不再触发任何后续回调。仅对流式响应（2xx + ResponseListener）有效。
     */
    void cancel();
}
