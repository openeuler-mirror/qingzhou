package qingzhou.api;

import java.io.IOException;

public interface AuthAdapter {
    void doAuth(String requestUri, AuthContext context);

    boolean logout(AuthContext context);

    interface AuthContext {
        String getParameter(String name);

        void setLoginSuccessful(String user, String... role);

        void redirect(String url);

        // 设置响应类型
        void setContentType(String contentType);

        void setHeader(String name, String value);

        void setHttpBody(byte[] body) throws IOException;
    }
}
