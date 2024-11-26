package qingzhou.ssh;

public interface SshClient extends SshSession {
    // 建立一个会话，可以以相同用户环境执行多条SSH命令，否则每次执行后自动清理登录现场
    SshSession createSession() throws Exception;
}
