package qingzhou.framework.ssh;

public class SSHConfig {
    private String hostname;
    private int port = 22;
    private String username;
    private String password;
    private String privateKeyLocation;
    private String keyPairType = "ssh-rsa";

    public String getHostname() {
        return hostname;
    }

    public SSHConfig setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public int getPort() {
        return port;
    }

    public SSHConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public SSHConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public SSHConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getPrivateKeyLocation() {
        return privateKeyLocation;
    }

    public SSHConfig setPrivateKeyLocation(String privateKeyLocation) {
        this.privateKeyLocation = privateKeyLocation;
        return this;
    }

    public String getKeyPairType() {
        return keyPairType;
    }

    public SSHConfig setKeyPairType(String keyPairType) {
        this.keyPairType = keyPairType;
        return this;
    }
}
