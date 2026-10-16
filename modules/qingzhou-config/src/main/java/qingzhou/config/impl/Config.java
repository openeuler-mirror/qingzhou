package qingzhou.config.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.config.remote.RemoteConfigSourceFactory;
import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;

@Component
public class Config {
    @Reference
    private ConfigurationAdmin configAdmin;

    @Reference
    private volatile HttpClient httpClient;

    @Reference
    private volatile Json json;

    private final String qzInstance = System.getProperty("qingzhou.instance");
    private final String qzVersion = System.getProperty("qingzhou.version");

    // 左侧空白裁剪：正则固定，预编译后复用，避免在每一行上重复编译（#34）
    private static final Pattern LEFT_TRIM = Pattern.compile("^[\\s　]+");

    // 在 qingzhou.command.cmd.StartArg 中反射引用
    public static Map<String, String> parse(String text) {
        Map<String, String> result = new LinkedHashMap<>();
        StringBuilder folded = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            for (String line; (line = reader.readLine()) != null; ) {
                line = LEFT_TRIM.matcher(line).replaceFirst(""); // 只 trim 左侧空白符，保留右侧，右侧的可能是业务需要的值
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("\\")) continue;

                if (line.endsWith("\\")) {// 折行
                    folded.append(line, 0, line.length() - 1);
                    continue;
                }
                String target = folded.append(line).toString();
                folded.setLength(0);

                int i = target.indexOf('=');
                String key = (i > 0 ? target.substring(0, i) : target).trim();
                if (!key.isEmpty()) result.put(key, i > 0 ? target.substring(i + 1).trim() : "");
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    @Activate
    public void init() throws Exception {
        if (qzInstance == null) {
            throw new IllegalStateException("system property qingzhou.instance must be set");
        }
        Path configFile = Paths.get(qzInstance, "conf", "qingzhou.properties");
        String configText = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
        Map<String, String> qzConfig = parse(configText);

        String enabled = qzConfig.get(RemoteConfigSourceFactory.KEY_PREFIX + "enabled");
        if (Boolean.parseBoolean(enabled)) { // 开启外部配置中心：远程覆盖本地，缺失保留
            try {
                RemoteConfigSourceFactory.create(qzConfig, httpClient, json).pull().entrySet().stream()
                        .filter(e -> !e.getKey().startsWith(RemoteConfigSourceFactory.KEY_PREFIX))
                        .forEach(e -> qzConfig.put(e.getKey(), e.getValue()));
            } catch (Throwable t) {
                // 配置错误即终止启动，不兜底，否则业务问题无法及早发现
                System.err.println("!!! Failed to pull remote configuration. Startup terminated !!! ");
                t.printStackTrace(System.err);
                System.exit(1);
            }
        }

        Map<String, Map<String, String>> osgiConfig = convertToOsgiConfig(qzConfig);
        distributeOsgiConfig(osgiConfig);
    }

    /**
     * 把 qingzhou.properties 中的键按 OSGi configurationPid 聚合。
     */
    private Map<String, Map<String, String>> convertToOsgiConfig(Map<String, String> qzConfig) {
        Map<String, Map<String, String>> configMap = new HashMap<>();
        for (String configKey : qzConfig.keySet()) {
            if (!configKey.startsWith("qingzhou-") && !configKey.startsWith("app~")) continue;

            int pidIndex = configKey.indexOf(".");
            if (pidIndex <= 0) { // 键须形如 "<pid>.<property>"：缺失时中止并给出键名，避免 substring(0, -1) 越界
                throw new IllegalStateException("invalid config key (expected '<pid>.<property>'): " + configKey);
            }
            configMap.computeIfAbsent(configKey.substring(0, pidIndex), pid -> new HashMap<>())
                    .put(configKey.substring(pidIndex + 1), qzConfig.get(configKey));
        }
        return configMap;
    }

    private void distributeOsgiConfig(Map<String, Map<String, String>> configMap) throws IOException {
        for (Map.Entry<String, Map<String, String>> entry : configMap.entrySet()) {
            String pid = entry.getKey();
            int i = pid.indexOf("~");// 含 ~ 为工厂配置
            Configuration configuration = i == -1
                    ? configAdmin.getConfiguration(pid, null)
                    : configAdmin.getFactoryConfiguration(pid.substring(0, i), pid.substring(i + 1), null);

            Hashtable<String, String> config = new Hashtable<>(entry.getValue());
            config.put("qingzhou.instance", qzInstance);
            config.put("qingzhou.version", qzVersion);
            configuration.update(config);
        }
    }
}
