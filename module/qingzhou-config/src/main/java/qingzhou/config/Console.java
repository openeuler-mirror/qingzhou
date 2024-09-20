package qingzhou.config;

import java.util.Arrays;
import java.util.Properties;

public class Console {
    private boolean enabled;
    private String contextRoot;
    private int port;
    private Security security;
    private User[] user;
    private Jmx jmx;

    public Properties getServletProperties() {
        return servletProperties;
    }

    public void setServletProperties(Properties servletProperties) {
        this.servletProperties = servletProperties;
    }

    private Properties servletProperties;

    public User getUser(String name) {
        if (user == null) return null;
        return Arrays.stream(user).filter(user -> user.getName().equals(name)).findAny().orElse(null);
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

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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
