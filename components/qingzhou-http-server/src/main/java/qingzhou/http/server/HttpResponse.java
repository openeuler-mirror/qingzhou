package qingzhou.http.server;

public interface HttpResponse {
    HttpResponse status(int status);

    HttpResponse header(String name, String value);

    HttpResponse contentType(String value);

    HttpResponse contentTypeJsonUtf8();

    HttpResponse send(String bodyAsUtf8);

    HttpResponse send(byte[] body);

    /**
     * 声明响应由调用方异步收尾：handler 返回后框架不再自动 {@link #finish()}，
     * 调用方写完响应后必须自行调用 {@link #finish()}。
     * 须在派发任何后台写线程之前调用，否则后台写入可能在自动收尾后失败。
     */
    HttpResponse async();

    /**
     * 结束响应：正常返回时框架会自动调用；异步模式（{@link #async()}）下由调用方负责。
     * 重复调用无副作用，之后不可再 {@link #send(byte[])}。
     */
    void finish();

    void redirect(String url);
}
