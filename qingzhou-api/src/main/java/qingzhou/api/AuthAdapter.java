package qingzhou.api;

public interface AuthAdapter {

    void authRequest(AuthContext context) throws Exception;

    void requestComplete(AuthContext context);

    interface AuthContext {
        String getParameter(String name);

        void setAuthState(AuthState state);

        void setUser(String user);

        void setFlag(String key, Object flag);

        <T> T getFlag(String key);
    }

    enum AuthState {
        LOGGED_IN, LOGGED_OUT
    }
}
