package qingzhou.http;

import java.util.Map;

public interface HttpClient {
    HttpResponse send(String url, byte[] body) throws Exception;

    HttpResponse send(String url, Map<String, String> params) throws Exception;
}
