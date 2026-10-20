package qingzhou.agent;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;

final class AgentDomainKeys {
    private final String domain;
    private final byte[] master;

    AgentDomainKeys(String instanceDir) {
        Path keyFile = Paths.get(instanceDir, "conf", "secret-key.properties");
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(keyFile)) {
            properties.load(in);
        } catch (Exception ignored) {
        }
        String prefix = "agent-domain~";
        String entry = null;
        for (String name : properties.stringPropertyNames()) {
            if (name.startsWith(prefix)) {
                entry = name;
                break;
            }
        }
        if (entry == null) {
            throw new IllegalStateException("no agent-domain root key configured in " + keyFile);
        }
        String rest = entry.substring(prefix.length());
        int dot = rest.indexOf('.'); // 兼容旧的 agent-domain~域名.版本 键名：版本已废弃
        domain = dot < 0 ? rest : rest.substring(0, dot);
        master = Base64.getDecoder().decode(properties.getProperty(entry).trim());
    }

    String domain() {
        return domain;
    }

    byte[] master() {
        return master;
    }
}
