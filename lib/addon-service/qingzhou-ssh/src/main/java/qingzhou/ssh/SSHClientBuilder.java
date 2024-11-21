package qingzhou.ssh;

public interface SSHClientBuilder {
    SSHClientBuilder host(String host);

    SSHClientBuilder port(int port);

    SSHClientBuilder username(String username);

    SSHClientBuilder password(String password);

    SSHClient build();
}
