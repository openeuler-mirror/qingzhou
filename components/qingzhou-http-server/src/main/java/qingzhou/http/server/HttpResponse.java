package qingzhou.http.server;

public interface HttpResponse {
    HttpResponse status(int status);

    HttpResponse header(String name, String value);

    HttpResponse contentType(String value);

    HttpResponse contentTypeJsonUtf8();

    HttpResponse send(String bodyAsUtf8);

    HttpResponse send(byte[] body);

    void finish();

    void redirect(String url);
}
