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
    private static final Set<String> appliedTags = ConcurrentHashMap.newKeySet();
    static boolean applied; // 全局配置仅可 apply 一次，测试可直接置位跳过

    private TinylogBootstrap() {
    }

    static synchronized void apply(String tag, Map<String, Object> ownConfig, ConfigurationAdmin configAdmin) {
        if (applied) {
            if (!appliedTags.contains(tag)) {
                System.err.println("logger '" + tag + "' configured after tinylog initialization, restart required to take effect");
            }
            return;
        }
        Map<String, Map<String, String>> perTagConfigs = gather(tag, ownConfig, configAdmin);
        org.tinylog.configuration.Configuration.replace(merge(perTagConfigs));
        appliedTags.addAll(perTagConfigs.keySet());
        applied = true; // 仅在配置真正生效后置位，避免失败后被后续激活静默跳过
    }

    // 收集全部日志器配置：tag -> 该日志器的字符串配置（全局项 + writer 项）
    private static Map<String, Map<String, String>> gather(String tag, Map<String, Object> ownConfig, ConfigurationAdmin configAdmin) {
        Map<String, Map<String, String>> result = new HashMap<>();
        try {
            Configuration[] configs = configAdmin.listConfigurations("(service.pid=qingzhou-logger*)");
            if (configs != null) {
                for (Configuration c : configs) {
                    result.put(LoggerImpl.parseTag(c.getPid()), toStrings(c.getProperties()));
                }
            }
        } catch (Exception e) {
            System.err.println("failed to list logger configurations: " + e.getMessage());
        }
        // 兜底：确保自身配置一定参与合并（ConfigAdmin 事件时序异常时不至于丢失当前日志器）
        result.put(tag, toStrings(new Hashtable<>(ownConfig)));
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
     * 合并为 tinylog 全局配置：非 writer 键仅取默认日志器；writer 键合并全部日志器，
     * 命名日志器的 writer 重命名并附加 tag，实现按 tag 路由到独立文件。
     */
    static Map<String, String> merge(Map<String, Map<String, String>> perTagConfigs) {
        Map<String, String> merged = new HashMap<>();
        merged.put("autoshutdown", "false");

        for (Map.Entry<String, Map<String, String>> entry : perTagConfigs.entrySet()) {
            String tag = entry.getKey();
            boolean isDefault = LoggerImpl.DEFAULT_INSTANCE.equals(tag); // todo 为何要对 default 特殊处理？能否移除？
            for (Map.Entry<String, String> w : entry.getValue().entrySet()) {
                String key = w.getKey();
                if (!key.startsWith("writer")) {
                    if (isDefault) merged.put(key, w.getValue());
                    continue;
                }
                String name = writerNameOf(key);
                String renamed = isDefault ? name : renameWriterName(name, tag);
                merged.put(renamed + key.substring(name.length()), w.getValue());
                merged.put(renamed + ".tag", tag);
            }
        }
        return merged;
    }

    private static String writerNameOf(String key) {
        int dot = key.indexOf('.');
        return dot == -1 ? key : key.substring(0, dot);
    }

    // writer_file -> writer_<tag>_file：确保以 writer 开头且各日志器间不冲突
    private static String renameWriterName(String writerName, String tag) {
        String suffix = writerName.substring("writer".length());
        if (suffix.startsWith("_")) suffix = suffix.substring(1);
        return suffix.isEmpty() ? "writer_" + tag : "writer_" + tag + "_" + suffix;
    }
}
