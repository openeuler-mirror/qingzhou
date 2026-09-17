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

    // 在 qingzhou.command.cmd.StartArg 中反射引用
    public static Map<String, String> parse(String text) {
        Map<String, String> result = new LinkedHashMap<>();
        StringBuilder folded = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            for (String line; (line = reader.readLine()) != null; ) {
                line = line.replaceAll("^[\\s\u3000]+", ""); // 只 trim 左侧空白符，保留右侧，右侧的可能是业务需要的值
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
        Path configFile = Paths.get(System.getProperty("qingzhou.instance"), "conf", "qingzhou.properties");
        String configText = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
        Map<String, String> qzConfig = parse(configText);

        String enabled = qzConfig.get(RemoteConfigSourceFactory.KEY_PREFIX + "enabled");
        if (Boolean.parseBoolean(enabled)) { // 开启外部配置中心：远程覆盖本地，缺失保留
            try {
                RemoteConfigSourceFactory.create(qzConfig, httpClient, json).pull().entrySet().stream()
                        .filter(e -> !e.getKey().startsWith(RemoteConfigSourceFactory.KEY_PREFIX))
                        .forEach(e -> qzConfig.put(e.getKey(), e.getValue()));
            } catch (Throwable t) {
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
            configuration.update(new Hashtable<>(entry.getValue()));
        }
    }
}
