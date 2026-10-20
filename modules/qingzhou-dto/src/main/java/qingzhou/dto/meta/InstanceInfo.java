package qingzhou.dto.meta;

import java.util.ArrayList;
import java.util.List;

public class InstanceInfo {
    // 以下字段由 registry 在认证通过后写入，不参与 JSON 收发
    private transient long lastRefreshTime;

    private String id;
    private String host;
    private int port;
    private String domain;
    private boolean sslEnabled;
    private String platformVersion;
    private final List<AppMeta> appMetas = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }

    public List<AppMeta> getAppMetas() {
        return appMetas;
    }

    public long getLastRefreshTime() {
        return lastRefreshTime;
    }

    public void setLastRefreshTime(long lastRefreshTime) {
        this.lastRefreshTime = lastRefreshTime;
    }
}
