package qingzhou.api;

import java.io.IOException;

public interface AuthAdapter {
    // 每次请求都进入此方法，在其中实现认证逻辑、登录逻辑、登出逻辑、错误处理逻辑、响应处理逻辑等
    void doAuth(String requestUri, AuthContext context) throws IOException;

    // 通过轻舟平台的注销接口退出登录时候调入此方法
    void logout(AuthContext context) throws IOException;

    interface AuthContext {
        String getParameter(String name);

        void loggedIn(String user, String... role) throws IOException;

        boolean isLoggedIn();

        void redirect(String url) throws IOException;

        // 设置响应类型，比如登录错误了，返回自定义的错误页面

        void responseContentType(String contentType);

        void responseHeader(String name, String value);

        void responseBody(byte[] body) throws IOException;
    }
}
