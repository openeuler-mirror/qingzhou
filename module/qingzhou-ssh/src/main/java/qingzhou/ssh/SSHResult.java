package qingzhou.ssh;

public interface SSHResult {
    boolean isSuccess();

    int getCode();

    String getMessage();
}
