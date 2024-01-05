package qingzhou.ssh;

public class SSHClientConfig {
    private String hostname;
    private int port = 22;
    private String username;
    private String password;
    private String privateKeyLocation;
    private String keyPairType = SSHClient.SSH_RSA;
    

    public SSHClientConfig() {
    }

    public String getHostname() {
        return hostname;
    }

    public SSHClientConfig setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public int getPort() {
        return port;
    }

    public SSHClientConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public SSHClientConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public SSHClientConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getPrivateKeyLocation() {
        return privateKeyLocation;
    }

    public SSHClientConfig setPrivateKeyLocation(String privateKeyLocation) {
        this.privateKeyLocation = privateKeyLocation;
        return this;
    }

    public String getKeyPairType() {
        return keyPairType;
    }

    public SSHClientConfig setKeyPairType(String keyPairType) {
        if (keyPairType != null) {
            this.keyPairType = keyPairType.trim();
        }
        return this;
    }
}
