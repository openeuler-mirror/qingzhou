package qingzhou.engine;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Properties;

class EnvCheck {
    // 必需配置缺失（文件缺失、键不存在、值为空白）即禁止启动，引导执行 init 完成初始化
    static void checkRequiredConfigs(String instanceDir) {
        File confDir = new File(instanceDir, "conf");
        File qzFile = new File(confDir, "qingzhou.properties");
        File scFile = new File(confDir, "secret-key.properties");
        Properties qzProps = load(qzFile);
        Properties scProps = load(scFile);

        // 该键声明了（含值为空）就必须有值；被注释或删除表示该能力未启用（如仅作 agent 的实例关闭登录），交由使用方按未配置处理
        String password = qzProps.getProperty("qingzhou-auth.password");
        if (password != null) require(password, "qingzhou-auth.password", qzFile);

        String cipherKeyName = qzProps.getProperty("qingzhou-crypto.global_cipher_key_name", "global");
        require(scProps.getProperty(cipherKeyName), cipherKeyName, scFile);

        // 声明 qingzhou-agent.url 即启用 agent：无域根密钥则 agent 端点全部 503、心跳无法加密，启动即失败
        if (qzProps.getProperty("qingzhou-agent.url") != null) requireAgentDomain(scProps, scFile);
    }

    private static Properties load(File file) {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file.toPath())) {
            properties.load(reader);
        } catch (IOException ignored) { // 缺失或读取失败同按未配置处理
        }
        return properties;
    }

    // 键格式须与 AgentDomainServiceImpl 解析一致：agent-domain~<domain>=<Base64 密钥>
    private static void requireAgentDomain(Properties secretProps, File file) {
        for (String name : secretProps.stringPropertyNames()) {
            if (name.startsWith("agent-domain~") && !secretProps.getProperty(name).trim().isEmpty()) return;
        }
        fail("agent-domain~<domain>", file);
    }

    private static void require(String value, String key, File file) {
        if (value == null || value.trim().isEmpty()) fail(key, file);
    }

    private static void fail(String key, File file) {
        String path = file.getParentFile().getParentFile().getName() + "/" + file.getName();
        System.err.println(key + " is missing in " + path + ", run bin/init.sh to initialize");
        System.exit(1);
    }
}
