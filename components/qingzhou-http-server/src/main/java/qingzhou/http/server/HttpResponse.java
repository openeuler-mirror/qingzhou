package qingzhou.http.server;

public interface HttpResponse {
    void status400Finish();

    void status500Finish(String msg);

    void redirect(String url);

    HttpResponse status(int status);

    HttpResponse header(String name, String value);

    HttpResponse contentType(String value);

    HttpResponse contentTypeJsonUtf8();

    HttpResponse send(String bodyAsUtf8);

    HttpResponse send(byte[] body);

    void finish();

    void sendFinish(String bodyAsUtf8);

    void sendFinish(byte[] body);
}
