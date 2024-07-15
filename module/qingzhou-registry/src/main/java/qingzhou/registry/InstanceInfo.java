package qingzhou.registry;

public class InstanceInfo {
    private String id;
    private String clusterId;
    private String host;
    private int port;
    private String key;
    private AppInfo[] appInfos;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
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
