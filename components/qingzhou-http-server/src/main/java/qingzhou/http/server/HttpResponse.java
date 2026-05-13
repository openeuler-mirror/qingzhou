package qingzhou.http.server;

public interface HttpResponse {
    void status500Finish(String msg);

    void status404Finish();

    void status400Finish();

    HttpResponse status(int status);

    HttpResponse header(String name, String value);

    HttpResponse contentType(String value);

    HttpResponse send(String bodyAsUtf8);

    HttpResponse send(byte[] body);

    void finish();

    void sendFinish(String bodyAsUtf8);

    void sendFinish(byte[] body);
}
