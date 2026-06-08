package qingzhou.http.client;

import java.util.Map;

public interface Request {
    Request method(HttpMethod method);

    Request headers(Map<String, String> headers);

    Request params(Map<String, String> params);

    Request body(byte[] body);

    Request files(Map<String, String> files);
}
