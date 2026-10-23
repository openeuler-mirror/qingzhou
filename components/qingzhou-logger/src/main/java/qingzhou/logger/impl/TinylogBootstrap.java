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
 *
 * <p>writer 键编码为 writer_&lt;名称&gt;_&lt;writer 后缀&gt;（名称中的 _ 翻倍转义防键碰撞），
 * 并附加 .tag=名称：tinylog 只把带对应 tag 的条目分发给该 writer，实现按日志器路由。
 * writer 的 tag 由框架占用，配置即失败；名称合法性约束见
 * {@link LoggerImpl#parseLoggerName(String)}。
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
                Object value = properties.get(key);
                // 契约适配：CM 属性值为 Object，框架属性 service.ranking 按 OSGi 契约为 Integer
                // （默认日志器的高优先级由 Config 自动注入），且本就不是 tinylog 配置项，跳过；
                // 其余配置值均为 String
                if (value instanceof String) {
                    config.put(key, (String) value);
                }
            }
            String loggerName = LoggerImpl.parseLoggerName(c.getPid());
            if (perTagConfigs.put(loggerName, config) != null) {
                // 如 qingzhou-logger 与 qingzhou-logger~default 同存时均解析为 default，
                // 不报错则后扫描到的配置静默覆盖前一个，其中一份 writer 配置整体失效
                throw new IllegalArgumentException("Logger configuration conflict: " + c.getPid() + " and the already loaded configuration resolve to the same logger " + loggerName);
            }
        }
        org.tinylog.configuration.Configuration.replace(merge(perTagConfigs));
        applied = true; // 仅在配置真正生效后置位，避免失败后被后续激活静默跳过
    }

    /**
     * 合并为 tinylog 全局配置：writer 键追加名称转义与 tag 后全量合并；非 writer 键（全局项）
     * 仅取默认日志器，避免取值随遍历顺序漂移。
     */
    private static Map<String, String> merge(Map<String, Map<String, String>> perTagConfigs) {
        Map<String, String> merged = new HashMap<>();
        merged.put("autoshutdown", "false");

        perTagConfigs.forEach((tag, config) -> config.forEach((k, v) -> {
            if (k.startsWith("writer")) {
                int dot = k.indexOf('.');
                String writerName = dot == -1 ? k : k.substring(0, dot);
                if (k.equals(writerName + ".tag")) {
                    throw new IllegalArgumentException("The writer's tag is occupied by the framework when routing by logger name, so it cannot be configured: " + k);
                }
                // 键编码为 writer_<名称>_<writer 后缀>，名称中的 _ 翻倍转义：保证
                // (日志器, writer) → 键 单射（如 app 与 app_default 的 writer 键不会互覆盖），
                // 且键中不含 . —— 否则 tinylog 无法将该键识别为 writer，日志静默丢失
                String writerNameForTag =
                        "writer_" + tag.replace("_", "__") + "_" + writerName.substring("writer".length());
                merged.put(writerNameForTag + k.substring(writerName.length()), v);
                merged.put(writerNameForTag + ".tag", tag); // 逐键重复 put 同一 tag（幂等），省去为去重额外维护 writer 名集合
            } else if (tag.equals(LoggerImpl.DEFAULT_LOGGER)) {
                // 全局项无法按 tag 拆分，只认默认日志器，避免取值随遍历顺序漂移
                merged.put(k, v);
            }
        }));
        return merged;
    }
}
