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
import qingzhou.common.FileUtil;
import qingzhou.crypto.Crypto;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainKeys;

@Component(configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AgentDomainKeysImpl implements AgentDomainKeys {
    @Reference
    private Crypto crypto;
    @Reference
    private Logger logger;

    private final Map<String, byte[]> masters = new HashMap<>();

    @Activate
    public void init(Map<String, String> config) throws IOException {
        Path keyFile = Paths.get(config.get("qingzhou.instance"), "conf", "secret-key.properties");
        // 域密钥是全平台信任根：不合规先告警再收紧；非 OSGi 构造时 logger 未注入
        FileUtil.restrictToOwnerOnly(keyFile.toFile(), msg -> {
            if (logger != null) logger.warn(msg);
        });
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(keyFile)) {
            properties.load(in);
        }
        String prefix = "agent-domain~";
        for (String name : properties.stringPropertyNames()) {
            if (!name.startsWith(prefix)) continue;
            String rest = name.substring(prefix.length());
            int dot = rest.indexOf('.');
            if (dot <= 0) { // 缺分隔符时的 substring 会越界，配置笔误不得表现为隐晦的运行期异常
                throw new IllegalArgumentException("invalid key name in secret-key.properties: " + name
                        + ", expected " + prefix + "<domain>.<item>");
            }
            masters.put(rest.substring(0, dot), crypto.getBase64Coder().decode(properties.getProperty(name).trim()));
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
    public byte[] toAgentAad(String instanceId, String instanceSecret, String[] reqData) {
        // 分隔符固定字段边界：裸拼接会让不同 (id, secret) 组合产出相同 AAD
        return crypto.generateCipherAad("REGISTRY_AGENT", instanceId, "|", instanceSecret, "|", Arrays.toString(reqData));
    }

    @Override
    public byte[] toRegistryAad(String instanceId, String... reqData) {
        return crypto.generateCipherAad("AGENT_REGISTRY", instanceId, Arrays.toString(reqData));
    }

    @Override
    public byte[] toRegistryResponseAad(String instanceId, String instanceSecret, String[] reqData) {
        return crypto.generateCipherAad("AGENT_REGISTRY", instanceId, "|", instanceSecret, "|", Arrays.toString(reqData));
    }
}
