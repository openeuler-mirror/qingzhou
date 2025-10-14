package qingzhou.ssh.impl;

import qingzhou.ssh.SshClient;
import qingzhou.ssh.SshClientBuilder;

import java.util.ArrayList;
import java.util.List;

public class SshClientBuilderImpl implements SshClientBuilder {
    private final SshConfig sshConfig = new SshConfig();
    private static final List<SshClientImpl> sshClientList = new ArrayList<>();

    public static List<SshClientImpl> getSshClientList() {
        return sshClientList;
    }

    @Override
    public SshClientBuilder host(String host) {
        sshConfig.hostname = host;
        return this;
    }

    @Override
    public SshClientBuilder port(int port) {
        sshConfig.port = port;
        return this;
    }

    @Override
    public SshClientBuilder username(String username) {
        sshConfig.username = username;
        return this;
    }

    @Override
    public SshClientBuilder password(String password) {
        sshConfig.password = password;
        return this;
    }

    @Override
    public SshClient build() {
        SshClientImpl sshClient = new SshClientImpl(sshConfig.clone());
        sshClientList.add(sshClient);
        sshClient.addSessionListener(() -> sshClientList.remove(sshClient));
        return sshClient;
    }
}
