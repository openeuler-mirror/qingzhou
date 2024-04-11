package qingzhou.config;

public class Remote {
    private boolean enabled;
    private String name;
    private String host;
    private int port;
    private Master[] master;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Master[] getMaster() {
        return master;
    }

    public void setMaster(Master[] master) {
        this.master = master;
    }
}
