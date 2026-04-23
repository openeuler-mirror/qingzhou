package qingzhou.http.client;

import java.util.List;
import java.util.Map;

public interface HttpResult {
    byte[] getBody();

    int getStatus();

    Map<String, List<String>> getHeaders();
}
