package qingzhou.http.client;

import java.util.Map;

public interface HttpClient {
    HttpResult request(String url, HttpMethod httpMethod, Map<String, String> params) throws Exception;
    HttpResult request(String url, HttpMethod httpMethod, Map<String, String> params, Map<String, String> headers) throws Exception;
    HttpResult request(String url, HttpMethod httpMethod, byte[] body, Map<String, String> headers) throws Exception;
}