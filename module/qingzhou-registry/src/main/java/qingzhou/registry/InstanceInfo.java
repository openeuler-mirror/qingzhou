package qingzhou.registry;

public class InstanceInfo {
    private String name;
    private String host;
    private int port;
    private String key;
    private AppInfo[] appInfos = new AppInfo[0];

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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public AppInfo[] getAppInfos() {
        return appInfos;
    }

    public void setAppInfos(AppInfo[] appInfos) {
        this.appInfos = appInfos;
    }
}
