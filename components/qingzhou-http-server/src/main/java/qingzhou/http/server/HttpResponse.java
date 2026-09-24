package qingzhou.http.server;

/**
 * 一次请求的响应写入口。
 * <p>
 * 底层 sink 为单发射者语义：所有方法须在<b>同一线程内串行</b>调用，不得并发写入，
 * 否则会触发 {@code FAIL_NON_SERIALIZED} 或乱序。
 * 终态方法（{@link #finish()}、各 sendFinish / statusXXXFinish、{@link #redirect(String)}）
 * 调用后响应即结束，不可再写入。
 */
public interface HttpResponse {
    void status400Finish();

    void status500Finish(String msg);

    /**
     * 以 302 重定向到目标地址。
     * <p>
     * 目标地址<b>必须由调用方校验</b>（是否为已登记或同源地址）。本方法只做写入、不校验目标，
     * 直接回显用户输入将构成开放重定向。
     */
    void redirect(String url);

    /**
     * @param status HTTP 状态码，须在 100–599 之间
     */
    HttpResponse status(int status);

    HttpResponse header(String name, String value);

    HttpResponse contentType(String value);

    HttpResponse contentTypeJsonUtf8();

    HttpResponse send(String bodyAsUtf8);

    HttpResponse send(byte[] body);

    /**
     * 结束响应。幂等：重复调用无副作用。
     */
    void finish();

    void sendFinish(String bodyAsUtf8);

    void sendFinish(byte[] body);
}
