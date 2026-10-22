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
import qingzhou.common.Constants;
import qingzhou.config.remote.RemoteConfigSourceFactory;
import qingzhou.json.Json;

@Component
public class Config {
    @Reference
    private ConfigurationAdmin configAdmin;

    @Reference
    private volatile Json json;

    private final String qzInstance = System.getProperty("qingzhou.instance");
    private final String qzVersion = System.getProperty("qingzhou.version");

    // 左侧空白裁剪：正则固定，预编译后复用，避免在每一行上重复编译（#34）
    private static final Pattern LEFT_TRIM = Pattern.compile("^[\\s　]+");
    // 拉取重试次数：吸收配置中心的瞬时抖动
    private static final int MAX_PULL_ATTEMPTS = 3;

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
        Path configFile = Paths.get(qzInstance, "conf", "qingzhou.properties");
        String configText = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
        Map<String, String> qzConfig = parse(configText);

        String enabled = qzConfig.get(RemoteConfigSourceFactory.KEY_PREFIX + "enabled");
        if (Boolean.parseBoolean(enabled)) { // 开启外部配置中心：远程覆盖本地，缺失保留
            pullRemoteConfig(qzConfig);
        }

        Map<String, Map<String, String>> osgiConfig = convertToOsgiConfig(qzConfig);
        distributeOsgiConfig(osgiConfig);
    }

    // 彻底失败仍终止启动：DS 激活是异步的，抛异常只会留下半启动实例（配置未分发、依赖组件未激活）而进程照旧运行
    private void pullRemoteConfig(Map<String, String> qzConfig) throws InterruptedException {
        for (int attempt = 1; ; attempt++) {
            try {
                RemoteConfigSourceFactory.create(qzConfig, json).pull().entrySet().stream()
                        .filter(e -> !e.getKey().startsWith(RemoteConfigSourceFactory.KEY_PREFIX))
                        .forEach(e -> qzConfig.put(e.getKey(), e.getValue()));
                return;
            } catch (Throwable t) {
                if (attempt >= MAX_PULL_ATTEMPTS) {
                    System.err.println("!!! Failed to pull remote configuration after " + MAX_PULL_ATTEMPTS + " attempts. Startup terminated !!! ");
                    t.printStackTrace(System.err);
                    System.exit(1);
                }

                System.err.println("!!! Failed to pull remote configuration (attempt " + attempt + "/" + MAX_PULL_ATTEMPTS
                        + "), retrying: " + t.getMessage());
                Thread.sleep(1000L * attempt); // 1s、2s 线性退避
            }
        }
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

            String factoryFlag = "~";
            int factoryIndex = pid.indexOf(factoryFlag);
            boolean isFactory = factoryIndex != -1;
            String factoryInstance = isFactory ? pid.substring(factoryIndex + factoryFlag.length()) : null;

            Configuration configuration = isFactory
                    ? configAdmin.getFactoryConfiguration(pid.substring(0, factoryIndex), factoryInstance, null)
                    : configAdmin.getConfiguration(pid, null);

            Hashtable<String, Object> config = new Hashtable<>();

            boolean hasRanking = false;
            for (Map.Entry<String, String> e : entry.getValue().entrySet()) {
                String key = e.getKey();
                if ("service.ranking".equals(key)) {
                    hasRanking = true;
                    try {
                        config.put(key, Integer.valueOf(e.getValue()));
                    } catch (NumberFormatException ignored) {
                        // 用户写错值时降级为 0，配置错误不应终止启动
                    }
                } else {
                    config.put(key, e.getValue());
                }
            }

            // default 实例约定：工厂配置中实例名为 default 的自动注入高 ranking，
            // 确保框架内无 target 的 @Reference 稳定选中它，如：@Reference Logger logger;
            // 用户显式设置的 ranking（包括写错值时降级为 0）不受此默认值覆盖
            if (isFactory && !hasRanking && Constants.CONFIG_FACTORY_DEFAULT_INSTANCE.equals(factoryInstance)) {
                config.put("service.ranking", 100);
            }

            config.put("qingzhou.instance", qzInstance);
            config.put("qingzhou.version", qzVersion);
            configuration.update(config);
        }
    }
}
