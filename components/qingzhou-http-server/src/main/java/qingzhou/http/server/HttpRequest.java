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

    // 认证主体：由认证层写入，handler 只读；未认证时为 null
    String getPrincipal();

    // 认证角色：返回副本，handler 只读；未认证时为 null
    String[] getRoles();
}
