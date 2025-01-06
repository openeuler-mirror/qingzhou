package qingzhou.api;

import java.io.IOException;

public interface AuthAdapter {
    void doAuth(String requestUri, AuthContext context) throws IOException;

    void logout(AuthContext context) throws IOException;

    interface AuthContext {
        String getParameter(String name);

        void loggedIn(String user, String... role) throws IOException;

        void redirect(String url) throws IOException;

        // 设置响应类型，比如登录错误了，返回自定义的错误页面

        void responseContentType(String contentType);

        void responseHeader(String name, String value);

        void responseBody(byte[] body) throws IOException;
    }
}
