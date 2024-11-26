package qingzhou.ssh;

public interface SshClientBuilder {
    SshClientBuilder host(String host);

    SshClientBuilder port(int port);

    SshClientBuilder username(String username);

    SshClientBuilder password(String password);

    SshClient build();
}
