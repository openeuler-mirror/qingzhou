package qingzhou.ssh;

public interface SshResult {
    boolean isSuccess();

    int getCode();

    String getMessage();
}
