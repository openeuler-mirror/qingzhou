package qingzhou.config.console;

public class Role {
    private String name;
    private String app;
    private String info;
    private boolean active;
    private String masterAppUris;
    private String uris;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getUris() {
        return uris;
    }

    public void setUris(String uris) {
        this.uris = uris;
    }

    public String getMasterAppUris() {
        return masterAppUris;
    }

    public void setMasterAppUris(String masterAppUris) {
        this.masterAppUris = masterAppUris;
    }
}
