package qingzhou.ssh;

public interface SshResult {
    Integer getStatus();

    void setStatus(Integer status);

    String getMessage();

    void setMessage(String message);

    boolean isSucceed();
}
