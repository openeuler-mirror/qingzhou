package qingzhou.registry.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = AgentDomainService.class)
public class AgentDomainService {
    private static final String PREFIX = "agent-domain~";

    private final Map<String, byte[]> masters = new HashMap<>();

    @Activate
    public void init(Map<String, String> config) throws IOException {
        Path keyFile = Paths.get(config.get("qingzhou.instance"), "conf", "secret-key.properties");
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(keyFile)) {
            properties.load(in);
        }
        for (String name : properties.stringPropertyNames()) {
            if (!name.startsWith(PREFIX)) continue;
            masters.put(domainOf(name), Base64.getDecoder().decode(properties.getProperty(name).trim()));
        }
    }

    /**
     * 域的根密钥；未配置该域时返回 null。
     */
    public byte[] master(String domainId) {
        return masters.get(domainId);
    }

    // 兼容旧的 agent-domain~域名.版本 键名：版本已废弃，取第一个 '.' 之前
    private static String domainOf(String name) {
        String rest = name.substring(PREFIX.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
