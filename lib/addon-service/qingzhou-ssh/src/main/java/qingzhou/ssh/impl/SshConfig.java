package qingzhou.ssh.impl;

class SshConfig implements Cloneable {
    String hostname;
    int port = 22;
    String username;
    String password;

    @Override
    public SshConfig clone() {
        try {
            return (SshConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
