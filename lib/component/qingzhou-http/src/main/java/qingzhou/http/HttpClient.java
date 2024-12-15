package qingzhou.http;

import java.util.Map;

public interface HttpClient {
    HttpResponse request(String url, HttpMethod httpMethod, Map<String, String> params) throws Exception;

    HttpResponse request(String url, HttpMethod httpMethod, Map<String, String> params, Map<String, String> headers) throws Exception;

    HttpResponse request(String url, HttpMethod httpMethod, byte[] body, Map<String, String> headers) throws Exception;
}
