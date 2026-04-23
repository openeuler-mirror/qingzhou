package qingzhou.config.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class Config {
    // 被 qingzhou.command.cmd.StartArg.parseConfig 反射使用
    public static Properties parseConfig(Path configFile) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configFile, StandardOpenOption.READ)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder tempLine = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) {
                line = line.replaceAll("^[\\s　]+", "");// 只 trim 左侧空白符，保留右侧，右侧的可能是业务需要的值得

                if (line.isEmpty()) continue;
                if (line.startsWith("#")) continue;
                if (line.equals("\\")) continue; // 只有一个换行符

                if (line.endsWith("\\")) { // 折行
                    tempLine.append(line, 0, line.length() - 1);
                    continue;
                } else {
                    tempLine.append(line);
                }

                String targetLine = tempLine.toString();
                tempLine.setLength(0); // 清空 折行 缓存
                int i = targetLine.indexOf("=");
                if (i > 0) {
                    String key = targetLine.substring(0, i).trim();
                    String val = targetLine.substring(i + 1).trim();
                    if (key.isEmpty()) continue;
                    properties.setProperty(key, val);
                } else {
                    properties.setProperty(targetLine, "");
                }
            }
        }
        return properties;
    }

    @Reference
    private ConfigurationAdmin configAdmin;

    @Activate
    public void init() throws IOException {
        Map<String, Map<String, String>> configMap = new HashMap<>();
        Path configFile = Paths.get(System.getProperty("qingzhou.instance"), "conf", "qingzhou.properties");
        Properties qzConfig = parseConfig(configFile);
        for (String configKey : qzConfig.stringPropertyNames()) {
            if (configKey.startsWith("qingzhou-")
                    || configKey.startsWith("app~")) {
                String configVal = qzConfig.getProperty(configKey);

                int pidIndex = configKey.indexOf(".");
                String configurationPid = configKey.substring(0, pidIndex); // OSGI cm configurationPid
                String moduleInternalKey = configKey.substring(pidIndex + 1);

                Map<String, String> moduleMap = configMap.computeIfAbsent(configurationPid, s -> new HashMap<>());
                moduleMap.put(moduleInternalKey, configVal);
            }
        }

        for (Map.Entry<String, Map<String, String>> entry : configMap.entrySet()) {
            String configurationPid = entry.getKey();
            Map<String, String> moduleMap = entry.getValue();

            Configuration configuration;
            int i = configurationPid.indexOf("~");
            if (i != -1) { // 工厂配置
                String factoryPid = configurationPid.substring(0, i);
                String name = configurationPid.substring(i + 1);
                configuration = configAdmin.getFactoryConfiguration(factoryPid, name, null);
            } else {
                configuration = configAdmin.getConfiguration(configurationPid, null);
            }

            configuration.update(new Hashtable<>(moduleMap));
        }
    }
}
