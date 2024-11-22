package qingzhou.ssh.impl;

import qingzhou.ssh.SSHClient;
import qingzhou.ssh.SSHClientBuilder;

import java.util.ArrayList;
import java.util.List;

public class SSHClientBuilderImpl implements SSHClientBuilder {
    private final SSHConfig sshConfig = new SSHConfig();
    private static final List<SSHClientImpl> sshClientList = new ArrayList<>();

    public static List<SSHClientImpl> getSshClientList() {
        return sshClientList;
    }

    @Override
    public SSHClientBuilder host(String host) {
        sshConfig.hostname = host;
        return this;
    }

    @Override
    public SSHClientBuilder port(int port) {
        sshConfig.port = port;
        return this;
    }

    @Override
    public SSHClientBuilder username(String username) {
        sshConfig.username = username;
        return this;
    }

    @Override
    public SSHClientBuilder password(String password) {
        sshConfig.password = password;
        return this;
    }

    @Override
    public SSHClient build() {
        SSHClientImpl sshClient = new SSHClientImpl(sshConfig.clone());
        sshClientList.add(sshClient);
        sshClient.addSessionListener(() -> sshClientList.remove(sshClient));
        return sshClient;
    }
}
