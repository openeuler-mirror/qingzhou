package qingzhou.ssh.impl;

class SSHConfig implements Cloneable {
    String hostname;
    int port = 22;
    String username;
    String password;

    @Override
    public SSHConfig clone() {
        try {
            return (SSHConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
