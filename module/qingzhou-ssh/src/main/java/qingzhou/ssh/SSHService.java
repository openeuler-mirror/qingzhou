package qingzhou.ssh;

import java.util.Map;

public interface SSHService {
    SSHSession buildSession(Map<String, String> config);
}
