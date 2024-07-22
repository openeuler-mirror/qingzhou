package qingzhou.config;

import java.util.Arrays;

public class Console {
    private boolean enabled;
    private String contextRoot;
    private int port;
    private Security security;
    private User[] user;
    private Department[] department;
    private Jmx jmx;

    public User getUser(String name) {
        if (user == null) return null;
        return Arrays.stream(user).filter(user -> user.getId().equals(name)).findAny().orElse(null);
    }

    public Department getDepartment(String id) {
        if (department == null) return null;
        return Arrays.stream(department).filter(d -> d.getId().equals(id)).findAny().orElse(null);
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

    public Department[] getDepartments() {
        return department;
    }

    public void setUser(Department[] department) {
        this.department = department;
    }
}
