package qingzhou.ssh;

import java.util.Map;

public interface SSHService {
    SSHClient buildClient(Map<String, String> config) throws Exception;
}
