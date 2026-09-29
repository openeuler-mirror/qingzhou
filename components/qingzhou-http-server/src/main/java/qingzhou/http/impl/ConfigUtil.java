package qingzhou.http.impl;

import java.util.Map;
import java.util.function.Function;

/**
 * qingzhou-http-server 配置的统一读取与校验入口。
 * 服务器、分发器、认证器三个组件同读一份配置：分散解析时新增一项校验极易漏掉某一处，
 * 而漏掉的那处恰好会在运行时静默降级（如窗口非正数令防爆破失效）。
 */
final class ConfigUtil {
    private static final String keyPrefix = "qingzhou-http-server.";

    private ConfigUtil() {
    }

    // 配置值下限校验：min=1 表示必须为正，min=0 表示可为 0 但不能为负
    static int atLeast(int value, int min, String key) {
        if (value < min)
            throw new IllegalArgumentException(keyPrefix + key + " must be >= " + min + ": " + value);
        return value;
    }

    static long atLeast(long value, long min, String key) {
        if (value < min)
            throw new IllegalArgumentException(keyPrefix + key + " must be >= " + min + ": " + value);
        return value;
    }

    // 配置值上界校验：防止数值参与后续运算时溢出（如窗口秒数 * 1000 变负令节流失效）
    static int atMost(int value, int max, String key) {
        if (value > max)
            throw new IllegalArgumentException(keyPrefix + key + " must be <= " + max + ": " + value);
        return value;
    }

    // 必填整数配置解析（无默认值场景）：失败报错带配置项名，与 getConfig 各分支口径一致
    static int parsePort(String val, String key) {
        int port;
        try {
            port = Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(invalid(key, val, "must be an integer"));
        }

        // 0 是合法值：交由操作系统分配空闲端口（测试与动态端口场景依赖它）
        if (port < 0 || port > 65535)
            throw new IllegalArgumentException(invalid(key, val, "out of range"));

        return port;
    }

    @SuppressWarnings("unchecked") // 按 defaultValue 的实际类型分派，四种分支各自类型安全
    static <T> T getConfig(Map<String, String> config, String key, T defaultValue) {
        String val = config.get(key);
        if (val == null) return defaultValue;
        val = val.trim(); // 值允许带前后空白，但不得因此解析失败或静默降级
        if (val.isEmpty()) return defaultValue;

        if (defaultValue instanceof String) return (T) val;
        // 数值解析失败直接抛出，暴露配置笔误而非静默回退默认值
        if (defaultValue instanceof Integer) return (T) number(val, key, Integer::parseInt);
        if (defaultValue instanceof Long) return (T) number(val, key, Long::parseLong);
        if (defaultValue instanceof Boolean) {
            // 布尔不得静默降级：非 true/false 一律报错，否则 ssl_enabled=TRUE/1 会被解析为关闭
            if ("true".equals(val) || "false".equals(val)) return (T) Boolean.valueOf(val);
            throw new IllegalArgumentException(invalid(key, val, "must be exactly 'true' or 'false' (case sensitive)"));
        }

        return defaultValue;
    }

    // 解析失败须带上配置项名：裸 NumberFormatException 无法定位是哪一项写错
    private static <N> N number(String val, String key, Function<String, N> parser) {
        try {
            return parser.apply(val);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(invalid(key, val, "must be an integer"));
        }
    }

    private static String invalid(String key, String val, String requirement) {
        return keyPrefix + key + " " + requirement + ", got: " + val;
    }
}
