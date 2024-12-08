package qingzhou.config;

import java.util.Arrays;

public class Console {
    private boolean enabled;
    private int port;
    private String contextRoot;
    private int maxPostSize;
    private Security security;
    private Jmx jmx;
    private OAuth oauth;
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public int getMaxPostSize() {
        return maxPostSize;
    }

    public void setMaxPostSize(int maxPostSize) {
        this.maxPostSize = maxPostSize;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public OAuth getOauth() {
        return oauth;
    }

    public void setOauth(OAuth oauth) {
        this.oauth = oauth;
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
