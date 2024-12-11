package qingzhou.http;

import java.util.Map;

public interface HttpClient {
    HttpResponse request(String url, HttpMethod httpMethod, Map<String, String> params) throws Exception;

    HttpResponse request(String url, HttpMethod httpMethod, Map<String, String> headers, Map<String, String> params) throws Exception;

    HttpResponse request(String url, HttpMethod httpMethod, Map<String, String> headers, byte[] body) throws Exception;
}
