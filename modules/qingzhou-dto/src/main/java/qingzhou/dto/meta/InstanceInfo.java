package qingzhou.dto.meta;

import java.util.ArrayList;
import java.util.List;

public class InstanceInfo {
    // 以下字段由 registry 在认证通过后写入，不参与 JSON 收发
    private transient volatile long lastRefreshTime; // 续期写入与健康检查读取跨线程，须保证可见性
    private transient String domain;
    private transient String id; // agent注册不带，由registry生成返回

    // 每实例随机密钥：agent 生成、仅随首次注册报文上报一次（已登记后心跳不再携带）。
    // 用于派生每实例加密密钥（见 AgentDomainService#getInstanceCipher）；
    private String instanceSecret;

    // agent 自报的对外可达地址：仅用于反代/NAT 部署，registry 未显式开启信任时忽略
    private String host;
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
