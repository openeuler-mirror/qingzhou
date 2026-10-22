package qingzhou.dto.meta;

import java.util.ArrayList;
import java.util.List;

public class InstanceInfo {
    // 以下字段由 registry 在认证通过后写入，不参与 JSON 收发
    private transient long lastRefreshTime;
    private transient String host;
    private transient String domain;

    private String id;
    // 每实例随机密钥：首次注册时由 agent 上报，作为 center→agent 方向的认证因子；随报文收发但不对外展示
    private String instanceSecret;
    // agent 自报的对外可达地址：仅用于反代/NAT 部署，registry 未显式开启信任时忽略
    private String advertisedHost;
    private int port;
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

    public String getAdvertisedHost() {
        return advertisedHost;
    }

    public void setAdvertisedHost(String advertisedHost) {
        this.advertisedHost = advertisedHost;
    }

    public String getInstanceSecret() {
        return instanceSecret;
    }

    public void setInstanceSecret(String instanceSecret) {
        this.instanceSecret = instanceSecret;
    }

    public long getRefreshTime() {
        return lastRefreshTime;
    }

    public void refreshTime() {
        this.lastRefreshTime = System.currentTimeMillis();
    }
}
