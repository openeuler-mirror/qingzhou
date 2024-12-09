package qingzhou.api;

public interface AuthAdapter {
    String getLoginUri();

    boolean login(AuthContext context) throws Exception;

    interface AuthContext {
        String getParameter(String name);

        void setUser(String user);
    }
}
