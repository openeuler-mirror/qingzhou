package qingzhou.api;

public interface AuthAdapter {
    void doAuth(String requestUri, AuthContext context);

    interface AuthContext {
        String getParameter(String name);

        void setLoginSuccessful(String user, String role);

        void redirect(String url);
    }
}
