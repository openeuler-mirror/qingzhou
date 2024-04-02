package qingzhou.ssh;

import qingzhou.framework.ssh.SSHResult;

public class SSHResultImp implements SSHResult {
    private String message;
    private int code;

    @Override
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setMessage(String message) {
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
