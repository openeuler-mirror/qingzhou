package qingzhou.registry.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Crypto;
import qingzhou.registry.AgentDomainKeys;

@Component(configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AgentDomainKeysImpl implements AgentDomainKeys {
    @Reference
    private Crypto crypto;

    private final Map<String, byte[]> masters = new HashMap<>();

    @Activate
    public void init(Map<String, String> config) throws IOException {
        Path keyFile = Paths.get(config.get("qingzhou.instance"), "conf", "secret-key.properties");
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(keyFile)) {
            properties.load(in);
        }
        String prefix = "agent-domain~";
        for (String name : properties.stringPropertyNames()) {
            if (name.startsWith(prefix)) {
                String rest = name.substring(prefix.length());
                int dot = rest.indexOf('.');
                String domain = rest.substring(0, dot);
                masters.put(domain, crypto.getBase64Coder().decode(properties.getProperty(name).trim()));
            }
        }
    }

    @Override
    public Set<String> getAllAgentDomains() {
        return masters.keySet();
    }

    @Override
    public byte[] getAgentDomainKey(String agentDomain) {
        return masters.get(agentDomain);
    }

    @Override
    public byte[] toAgentAad(String instanceId, String... reqData) {
        return crypto.generateCipherAad("REGISTRY_AGENT", instanceId, Arrays.toString(reqData));
    }

    @Override
    public byte[] toRegistryAad(String instanceId, String... reqData) {
        return crypto.generateCipherAad("AGENT_REGISTRY", instanceId, Arrays.toString(reqData));
    }
}
