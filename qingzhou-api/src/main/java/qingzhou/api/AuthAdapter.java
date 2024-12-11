package qingzhou.api;

import java.io.IOException;

public interface AuthAdapter {
    void doAuth(String requestUri, AuthContext context) throws Exception;

    interface AuthContext {
        String getParameter(String name);

        void setLoginSuccessful(String user) throws IOException;

        void redirect(String url) throws IOException;
    }
}
