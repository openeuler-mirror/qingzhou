package qingzhou.config;

import java.util.Arrays;

public class Console {
    private boolean enabled;
    private Security security;
    private Web web;
    private Jmx jmx;
    private OAuth2 oAuth2;
    private User[] user;
    private Role[] role;


    public User getUser(String name) {
        if (user == null) return null;
        return Arrays.stream(user).filter(user -> user.getName().equals(name)).findAny().orElse(null);
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

    public OAuth2 getOAuth2() {
        return oAuth2;
    }

    public void setOAuth2(OAuth2 oAuth2) {
        this.oAuth2 = oAuth2;
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
