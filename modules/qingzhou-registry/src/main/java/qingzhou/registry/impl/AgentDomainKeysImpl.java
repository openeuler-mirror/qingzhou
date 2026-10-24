package qingzhou.registry.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import qingzhou.crypto.EpochKeyRotatingCipher;
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
            byte[] domainKey = crypto.getBase64Coder().decode(properties.getProperty(name).trim());
            // 域根密钥是全平台信任根：HKDF 无法补救低熵 IKM，至少 16 字节随机，激活即校验
            if (domainKey.length < 16) {
                throw new IllegalArgumentException("agent domain key too short (>=16 bytes): " + name);
            }
            masters.put(rest.substring(0, dot), domainKey);
        }
    }

    @Override
    public Set<String> getAllAgentDomains() {
        // 不可变视图：外部不得增删信任根表；masters 仅在激活期写入，运行期只读
        return Collections.unmodifiableSet(masters.keySet());
    }

    @Override
    public EpochKeyRotatingCipher getDomainCipher(String agentDomain) {
        return crypto.getEpochKeyRotatingCipher(requireDomainKey(agentDomain));
    }

    @Override
    public EpochKeyRotatingCipher getInstanceCipher(String agentDomain, String instanceSecret) {
        return crypto.getEpochKeyRotatingCipher(deriveInstanceKey(requireDomainKey(agentDomain), instanceSecret));
    }

    private byte[] requireDomainKey(String agentDomain) {
        byte[] domainKey = masters.get(agentDomain);
        if (domainKey == null) { // 未知域名：fail-fast，避免 NPE 掩盖配置/调用错误
            throw new IllegalArgumentException("unknown agent domain: " + agentDomain);
        }
        return domainKey;
    }

    // 由域密钥与实例密钥派生每实例主密钥：域密钥(定长)与实例密钥(定长 UUID)拼接无歧义，SHA-256 作 KDF 足够
    private byte[] deriveInstanceKey(byte[] domainKey, String instanceSecret) {
        byte[] secret = instanceSecret.getBytes(StandardCharsets.UTF_8);
        byte[] mix = new byte[domainKey.length + secret.length];
        System.arraycopy(domainKey, 0, mix, 0, domainKey.length);
        System.arraycopy(secret, 0, mix, domainKey.length, secret.length);
        return crypto.getMessageDigest().sha256(mix);
    }

    @Override
    public byte[] toAgentAad(String instanceId, String instanceSecret, String[] reqData) {
        return frame("REGISTRY_AGENT", instanceId, instanceSecret, reqData);
    }

    @Override
    public byte[] toRegistryAad(String instanceId, String... reqData) {
        return frame("AGENT_REGISTRY", instanceId, null, reqData);
    }

    @Override
    public byte[] toRegistryResponseAad(String instanceId, String instanceSecret, String[] reqData) {
        return frame("AGENT_REGISTRY", instanceId, instanceSecret, reqData);
    }

    // 长度前缀分帧：每个字段先写长度再写内容，字段与数组边界均无歧义，
    // 杜绝不同 (tag, instanceId, instanceSecret, reqData) 组合拼接出相同 AAD（如 ["a,b"] 与 ["a","b"]）
    private byte[] frame(String tag, String instanceId, String instanceSecret, String[] reqData) {
        StringBuilder sb = new StringBuilder();
        append(sb, tag);
        append(sb, instanceId);
        append(sb, instanceSecret);
        sb.append(reqData.length).append(':');
        for (String item : reqData) append(sb, item);
        return crypto.generateCipherAad(sb.toString());
    }

    private static void append(StringBuilder sb, String field) {
        sb.append(field == null ? -1 : field.length()).append(':');
        if (field != null) sb.append(field);
    }
}
