package qingzhou.ssh.impl;

import qingzhou.ssh.SSHResult;

class SSHResultImp implements SSHResult {
    private String message;
    private int code;

    @Override
    public int getCode() {
        return code;
    }

    void setCode(int code) {
        this.code = code;
    }

    void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean isSuccess() {
        return code == 0;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
