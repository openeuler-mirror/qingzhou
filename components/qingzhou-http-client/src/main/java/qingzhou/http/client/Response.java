package qingzhou.http.client;

public interface Response {
    int getStatus();

    byte[] getBody();

    /**
     * 取消本次请求：断开底层连接，不再触发任何后续回调。
     */
    void cancel();
}
