package qingzhou.logger.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * tinylog 全局配置的一次性引导：聚合 qingzhou-logger~<名称>.* 配置后合并 apply。
 * tinylog 2.6.2 之后配置冻结不可变更，因此运行期新增的日志器配置须重启生效。
 */
final class TinylogBootstrap {
    static final Set<String> appliedTags = ConcurrentHashMap.newKeySet(); // 非空即表示全局配置已生效

    static synchronized void apply(String tag, ConfigurationAdmin configAdmin) {
        if (!appliedTags.isEmpty()) {
            if (!appliedTags.contains(tag))
                System.err.println("logger '" + tag + "' configured after tinylog initialization, restart required to take effect");
            return;
        }
        Map<String, Map<String, String>> perTagConfigs = gather(configAdmin);
        org.tinylog.configuration.Configuration.replace(merge(perTagConfigs));
        appliedTags.addAll(perTagConfigs.keySet()); // 仅在配置真正生效后置位，避免失败后被后续激活静默跳过
    }

    // 收集全部日志器配置：tag -> 该日志器的字符串配置（全局项 + writer 项）
    private static Map<String, Map<String, String>> gather(ConfigurationAdmin configAdmin) {
        Map<String, Map<String, String>> result = new HashMap<>();
        try {
            Configuration[] configs = configAdmin.listConfigurations("(service.pid=qingzhou-logger*)");
            if (configs != null) {
                for (Configuration c : configs) {
                    result.put(LoggerImpl.parseLoggerName(c.getPid()), toStrings(c.getProperties()));
                }
            }
        } catch (Exception e) {
            System.err.println("failed to list logger configurations: " + e.getMessage());
        }
        return result;
    }

    private static Map<String, String> toStrings(Dictionary<String, Object> properties) {
        Map<String, String> map = new HashMap<>();
        if (properties == null) return map;
        for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements(); ) {
            String key = keys.nextElement();
            Object value = properties.get(key);
            if (value instanceof String) map.put(key, (String) value);
        }
        return map;
    }

    /**
     * 合并为 tinylog 全局配置：非 writer 键属 tinylog 全局项，仅取默认日志器；writer 键合并全部日志器，
     * 重命名并附加 tag，实现按 tag 路由到独立文件。
     */
    static Map<String, String> merge(Map<String, Map<String, String>> perTagConfigs) {
        Map<String, String> merged = new HashMap<>();
        merged.put("autoshutdown", "false");

        // 全局项无法按 tag 拆分，只认默认日志器，避免取值随遍历顺序漂移
        perTagConfigs.get(LoggerImpl.DEFAULT_LOGGER).forEach((k, v) -> {
            if (!k.startsWith("writer")) merged.put(k, v);
        });
        perTagConfigs.forEach((tag, config) -> config.forEach((k, v) -> {
            if (!k.startsWith("writer")) return;
            int dot = k.indexOf('.');
            String name = dot == -1 ? k : k.substring(0, dot);
            String renamed = renameWriterName(name, tag);
            merged.put(renamed + k.substring(name.length()), v);
            merged.put(renamed + ".tag", tag);
        }));
        return merged;
    }

    // writer_file -> writer_<tag>_file：确保以 writer 开头且各日志器间不冲突
    private static String renameWriterName(String writerName, String tag) {
        String suffix = writerName.substring("writer".length());
        return "writer_" + tag + (suffix.isEmpty() || suffix.startsWith("_") ? suffix : "_" + suffix);
    }
}
