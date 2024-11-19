package qingzhou.http;

import java.util.List;
import java.util.Map;

public interface HttpResponse {
    byte[] getResponseBody();

    int getResponseCode();

    Map<String, List<String>> getResponseHeaders();
}
