package qingzhou.http.server;

public interface HttpResponse {
    HttpResponse statusError();

    HttpResponse statusNotFound();

    HttpResponse statusBad();


    HttpResponse status(int status);

    HttpResponse header(String name, String value);

    HttpResponse contentType(String value);

    HttpResponse contentTypeHtmlUtf8();

    HttpResponse contentTypeJsonUtf8();

    HttpResponse contentTypeStream();

    /***************** 写入并结束响应 ****************/

    HttpResponse sendResponse(String bodyAsUtf8);

    HttpResponse sendResponse(byte[] body);
}
