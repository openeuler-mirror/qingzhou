package qingzhou.http.client;

import java.util.Map;

public interface Request {
    Request method(HttpMethod method);

    Request header(String key, String val);

    Request headers(Map<String, String> headers);

    Request params(Map<String, String> params);

    Request body(byte[] body);

    Request files(Map<String, String> files);
}
