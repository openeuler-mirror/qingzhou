package qingzhou.http.server;

import java.util.List;
import java.util.Map;

public interface HttpRequest {
    String getRemoteHost();
    String getPath();
    String getFullPath();
    String getParameter(String name);
    Map<String, List<String>> getParameters();
    String getHeader(String header);
    String getContentType();
    String getMethod();
    boolean isFormUrlencoded();
    byte[] getBody();
}