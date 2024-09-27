package qingzhou.config;

import java.util.Arrays;

public class Console {
    private boolean enabled;
    private Web web;
    private Jmx jmx;
    private Security security;
    private User[] user;

    public User getUser(String name) {
        if (user == null) return null;
        return Arrays.stream(user).filter(user -> user.getName().equals(name)).findAny().orElse(null);
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

    public User[] getUser() {
        return user;
    }

    public void setUser(User[] user) {
        this.user = user;
    }
}
