package qingzhou.http;

import java.util.Map;

public interface HttpClient {
    HttpResponse get(String url, Map<String, String> headers) throws Exception;

    HttpResponse post(String url, Map<String, String> params) throws Exception;

    HttpResponse post(String url, Map<String, String> params, Map<String, String> headers) throws Exception;

    HttpResponse post(String url, byte[] body, Map<String, String> headers) throws Exception;
}
