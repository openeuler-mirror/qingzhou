package qingzhou.http.server;

public interface HttpResponse {
    HttpResponse statusError();

    HttpResponse statusNotFound();

    HttpResponse statusBad();

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
