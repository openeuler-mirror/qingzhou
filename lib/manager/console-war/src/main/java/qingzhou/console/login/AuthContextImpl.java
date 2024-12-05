package qingzhou.console.login;

import qingzhou.api.AuthAdapter;

import java.util.HashMap;
import java.util.Map;

public class AuthContextImpl implements AuthAdapter.AuthContext {
    private final Parameter parameter;
    private AuthAdapter.AuthState authState;
    private String user;
    private final Map<String, Object> flags = new HashMap<>();

    public AuthContextImpl(Parameter parameter) {
        this.parameter = parameter;
    }

    @Override
    public String getParameter(String name) {
        return parameter.get(name);
    }

    @Override
    public void setAuthState(AuthAdapter.AuthState state) {
        this.authState = state;
    }

    @Override
    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public void setFlag(String key, Object flag) {
        flags.put(key, flag);
    }

    AuthAdapter.AuthState getAuthState() {
        return authState;
    }

    String getUser() {
        return user;
    }

    @Override
    public <T> T getFlag(String key) {
        return (T) flags.get(key);
    }
}
