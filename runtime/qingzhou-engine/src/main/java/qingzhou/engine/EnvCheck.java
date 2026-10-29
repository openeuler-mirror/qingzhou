package qingzhou.engine;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Properties;

class EnvCheck {
    // 必需配置缺失（文件缺失、键不存在、值为空白）即禁止启动，引导执行 init 完成初始化
    static void checkRequiredConfigs(String instanceDir) {
        String filePath = "instances/" + new File(instanceDir).getName() + "/conf/"; // 相对 instances 展示，便于用户定位
        File confDir = new File(instanceDir, "conf");
        File propsFile = new File(confDir, "qingzhou.properties");
        File secretFile = new File(confDir, "secret-key.properties");
        Properties qzProps = loadProperties(propsFile);
        requireIfDeclared(qzProps, "qingzhou-auth.password", filePath + propsFile.getName());
        require(loadProperties(secretFile),
                qzProps.getProperty("qingzhou-crypto.global_cipher_key_name", "global"),
                filePath + secretFile.getName());
    }

    private static Properties loadProperties(File file) {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file.toPath())) {
            properties.load(reader);
        } catch (IOException ignored) { // 缺失或读取失败同按未配置处理
        }
        return properties;
    }

    // 该键声明了（含值为空）就必须有值；被注释或删除表示该能力未启用（如仅作 agent 的实例关闭登录），交由使用方按未配置处理
    private static void requireIfDeclared(Properties properties, String key, String path) {
        String value = properties.getProperty(key);
        if (value == null) return;
        requireValue(value, key, path);
    }

    private static void require(Properties properties, String key, String path) {
        requireValue(properties.getProperty(key), key, path);
    }

    private static void requireValue(String value, String key, String path) {
        if (value == null || value.trim().isEmpty()) {
            System.err.println(key + " is missing in " + path + ", run bin/init.sh to initialize");
            System.exit(1);
        }
    }
}
