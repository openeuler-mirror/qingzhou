package qingzhou.logger.impl;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * tinylog 全局配置的一次性引导：聚合 qingzhou-logger~<名称>.* 配置后合并 apply。
 * tinylog 2.6.2 之后配置冻结不可变更，因此运行期新增的日志器配置须重启生效。
 */
final class TinylogBootstrap {
    private static boolean applied; // 全局配置是否已生效

    static synchronized void apply(ConfigurationAdmin configAdmin) throws Exception {
        if (configAdmin == null) return; // 非 OSGi 环境（无配置管理服务）：沿用已加载的 tinylog 配置
        if (applied) return;
        Map<String, Map<String, String>> perTagConfigs = new HashMap<>();
        for (Configuration c : configAdmin.listConfigurations("(service.pid=qingzhou-logger*)")) {
            Map<String, String> config = new HashMap<>();
            Dictionary<String, Object> properties = c.getProperties();
            for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements(); ) {
                String key = keys.nextElement();
                config.put(key, (String) properties.get(key)); // 非 String 值直接失败，不静默丢弃
            }
            perTagConfigs.put(LoggerImpl.parseLoggerName(c.getPid()), config);
        }
        org.tinylog.configuration.Configuration.replace(merge(perTagConfigs));
        applied = true; // 仅在配置真正生效后置位，避免失败后被后续激活静默跳过
    }

    /**
     * 合并为 tinylog 全局配置：非 writer 键（全局项）仅取默认日志器；writer 键合并全部日志器，
     * 重命名并附加 tag，实现按 tag 路由到独立文件。
     */
    private static Map<String, String> merge(Map<String, Map<String, String>> perTagConfigs) {
        Map<String, String> merged = new HashMap<>();
        merged.put("autoshutdown", "false");

        perTagConfigs.forEach((tag, config) -> config.forEach((k, v) -> {
            if (k.startsWith("writer")) {
                int dot = k.indexOf('.');
                String writerName = dot == -1 ? k : k.substring(0, dot);
                String writerNameForTag = "writer_" + tag + writerName.substring("writer".length()); // 附加 tag，确保各日志器的 writer 键互不冲突
                merged.put(writerNameForTag + k.substring(writerName.length()), v);
                merged.put(writerNameForTag + ".tag", tag);
            } else if (tag.equals(LoggerImpl.DEFAULT_LOGGER)) {
                // 全局项无法按 tag 拆分，只认默认日志器，避免取值随遍历顺序漂移
                merged.put(k, v);
            }
        }));
        return merged;
    }
}
