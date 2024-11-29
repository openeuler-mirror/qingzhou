package qingzhou.core.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class Console {
    private boolean enabled;
    private Web web;
    private Jmx jmx;
    private Security security;
    private User[] user;
    private static final List<User> oauthUsers = new ArrayList<>();
    private Role[] role;


    public User getUser(String name) {
        if (user == null) return null;
        return Stream.concat(Arrays.stream(user), oauthUsers.stream()).filter(user -> user.getName().equals(name)).findAny().orElse(null);
    }

    public List<User> getOauthUsers() {
        return oauthUsers;
    }

    public User[] getUser() {
        return user;
    }

    public void setUser(User[] user) {
        this.user = user;
    }

    public Web getWeb() {
        return web;
    }

    public void setWeb(Web web) {
        this.web = web;
    }

    public Jmx getJmx() {
        return jmx;
    }

    public void setJmx(Jmx jmx) {
        this.jmx = jmx;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Role[] getRole() {
        return role;
    }

    public void setRole(Role[] role) {
        this.role = role;
    }

    public Role getRole(String name) {
        if (role == null) return null;
        return Arrays.stream(role).filter(role -> role.getName().equals(name)).findAny().orElse(null);
    }

}
