package qingzhou.console.login;

import qingzhou.api.AuthAdapter;

public class AuthContextImpl implements AuthAdapter.AuthContext {
    private final Parameter parameter;
    private String user;

    public AuthContextImpl(Parameter parameter) {
        this.parameter = parameter;
    }

    @Override
    public String getParameter(String name) {
        return parameter.get(name);
    }

    @Override
    public void setUser(String user) {
        this.user = user;
    }

    String getUser() {
        return user;
    }
}
