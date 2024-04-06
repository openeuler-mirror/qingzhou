package qingzhou.framework.ssh;

public interface SSHClient extends SSHSession {
    // 建立一个会话，可以以相同用户环境执行多条SSH命令，否则每次执行后自动清理登录现场
    SSHSession createSession() throws Exception;
}
