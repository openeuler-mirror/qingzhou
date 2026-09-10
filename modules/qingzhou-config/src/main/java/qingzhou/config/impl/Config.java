package qingzhou.config.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.config.remote.ConfigText;
import qingzhou.config.remote.RemoteConfigSource;
import qingzhou.config.remote.RemoteConfigSourceFactory;
import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;

@Component
public class Config {
    @Reference
    private ConfigurationAdmin configAdmin;
    @Reference
    private HttpClient httpClient;
    @Reference
    private Json json;

    // 被 qingzhou.command.cmd.StartArg.parseConfig 反射使用
    public static Properties parseConfig(Path configFile) throws IOException {
        Properties properties = new Properties();
        properties.putAll(ConfigText.parse(new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8)));
        return properties;
    }

    @Activate
    public void init() throws Exception {
        Properties qzConfig = parseConfig(
                Paths.get(System.getProperty("qingzhou.instance"), "conf", "qingzhou.properties"));

        Map<String, Map<String, String>> configMap = converToOsgiConfig(qzConfig);

        RemoteConfigSource remote = RemoteConfigSourceFactory.create(qzConfig, httpClient, json);
        if (remote != null) { // 开启外部配置中心：远程覆盖本地，缺失保留
            merge(configMap, remote.pull());
        }

        distributeOsgiConfig(configMap);
    }

    /** 远程配置以本地为底、逐 key 覆盖；远程缺失的本地 key 保留，且不得改写 qingzhou-config 自举参数。 */
    private void merge(Map<String, Map<String, String>> configMap, Map<String, Map<String, String>> remoteConfig) {
        for (Map.Entry<String, Map<String, String>> entry : remoteConfig.entrySet()) {
            if ("qingzhou-config".equals(entry.getKey())) continue;
            configMap.computeIfAbsent(entry.getKey(), pid -> new HashMap<>()).putAll(entry.getValue());
        }
    }

    /** 把 qingzhou.properties 中的键按 OSGi configurationPid 聚合。 */
    private Map<String, Map<String, String>> converToOsgiConfig(Properties qzConfig) {
        Map<String, Map<String, String>> configMap = new HashMap<>();
        for (String configKey : qzConfig.stringPropertyNames()) {
            if (!configKey.startsWith("qingzhou-") && !configKey.startsWith("app~")) continue;

            int pidIndex = configKey.indexOf(".");
            configMap.computeIfAbsent(configKey.substring(0, pidIndex), pid -> new HashMap<>())
                    .put(configKey.substring(pidIndex + 1), qzConfig.getProperty(configKey));
        }
        return configMap;
    }

    private void distributeOsgiConfig(Map<String, Map<String, String>> configMap) throws IOException {
        for (Map.Entry<String, Map<String, String>> entry : configMap.entrySet()) {
            String configurationPid = entry.getKey();
            int i = configurationPid.indexOf("~");

            Configuration configuration;
            if (i != -1) { // 工厂配置
                configuration = configAdmin.getFactoryConfiguration(
                        configurationPid.substring(0, i), configurationPid.substring(i + 1), null);
            } else {
                configuration = configAdmin.getConfiguration(configurationPid, null);
            }
            configuration.update(new Hashtable<>(entry.getValue()));
        }
    }
}
