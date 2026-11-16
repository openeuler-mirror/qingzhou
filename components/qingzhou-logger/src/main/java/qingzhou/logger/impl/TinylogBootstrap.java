package qingzhou.logger.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tinylog 全局配置的一次性引导：按 qingzhou-logger.loggers 声明的日志器展开 writer。
 * tinylog 2.6.2 之后配置冻结不可变更，因此运行期新增的日志器须重启生效。
 *
 * <p>writer 键编码为 writer_&lt;名称&gt;_&lt;writer 后缀&gt;（名称中的 _ 翻倍转义防键碰撞），
 * 并附加 .tag=&lt;名称&gt;：tinylog 只把带对应 tag 的条目分发给该 writer，实现按日志器路由。</p>
 */
final class TinylogBootstrap {
    private static boolean applied; // 全局配置是否已生效

    static synchronized void apply(Map<String, String> config, List<String> names) {
        if (applied) return; // 若配置了 ~ 分隔的 OSGI 多实例，这里就会进入多次
        org.tinylog.configuration.Configuration.replace(build(config, names));
        applied = true; // 仅在配置真正生效后置位，避免失败后被后续激活静默跳过
    }

    private static Map<String, String> build(Map<String, String> config, List<String> names) {
        Map<String, String> merged = new HashMap<>();
        merged.put("autoshutdown", "false");

        String tinyWriterPrefix = "writer";
        for (String name : names) {
            String prefix = name + ".";
            for (Map.Entry<String, String> entry : config.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith(prefix)) continue;
                String strippedKey = key.substring(prefix.length());
                if (strippedKey.startsWith(tinyWriterPrefix)) {
                    // 键编码为 writer_<名称>_<writer 后缀>，名称中的 _ 翻倍转义，保证 (日志器, writer) → 键 单射；
                    // 键中不得含额外 . —— 否则 tinylog 无法识别为 writer，日志静默丢失
                    int dot = strippedKey.indexOf('.');
                    String tinyWriterName = dot == -1 ? strippedKey : strippedKey.substring(0, dot);
                    String newNamedTinyWriterName = "writer_" + name.replace("_", "__") + "_" + tinyWriterName.substring(tinyWriterPrefix.length());
                    merged.put(newNamedTinyWriterName + strippedKey.substring(tinyWriterName.length()), entry.getValue());
                    merged.put(newNamedTinyWriterName + ".tag", name); // 逐键重复 put 同一 tag（幂等），省去 writer 名集合
                } else if (name.equals(LoggerImpl.DEFAULT_LOGGER)) {
                    // 全局项无法按日志器拆分，只认默认日志器，避免取值随遍历顺序漂移
                    merged.put(strippedKey, entry.getValue());
                }
            }
        }

        return merged;
    }
}
