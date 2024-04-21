package qingzhou.http;

import java.util.List;
import java.util.Map;

public interface HttpResponse {
    String getResponseBody();

    int getResponseCode();

    Map<String, List<String>> getResponseHeaders();
}
