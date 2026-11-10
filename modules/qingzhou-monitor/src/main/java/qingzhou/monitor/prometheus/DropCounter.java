package qingzhou.monitor.prometheus;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** 单次抓取的丢弃统计：按原因计数并保留首个明细，供日志与自监控指标使用。 */
public final class DropCounter {
    public enum Reason {
        /** 字段值不是有限数值（含单位、时间、逗号拼接的多值文本） */
        NON_NUMERIC,
        /** 字段值不是标量（Map/List 等），无法表达为单一样本 */
        NON_SCALAR,
        /** 指标名占用了端点自监控命名空间 */
        RESERVED_NAME,
        /** 提供方标签占用了端点内置标签 */
        RESERVED_LABEL,
        /** 同一指标名与标签组合重复输出，Prometheus 会判定重复序列 */
        DUPLICATE_SERIES,
        /** 同名指标类型不一致，exposition 只允许一种类型 */
        TYPE_CONFLICT
    }

    private final Map<Reason, Long> counts = new EnumMap<>(Reason.class);
    private String first;

    public void add(Reason reason, String detail) {
        Long count = counts.get(reason);
        counts.put(reason, count == null ? 1 : count + 1);
        if (first == null) first = detail;
    }

    Map<Reason, Long> counts() {
        return Collections.unmodifiableMap(counts);
    }

    long total() {
        long total = 0;
        for (Long count : counts.values()) total += count;
        return total;
    }

    /** 形如 "non_numeric=3, duplicate_series=1"，用于一次性告警日志 */
    String describe() {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<Reason, Long> entry : counts.entrySet()) {
            if (text.length() > 0) text.append(", ");
            text.append(label(entry.getKey())).append('=').append(entry.getValue());
        }
        return text.toString();
    }

    String first() {
        return first;
    }

    static String label(Reason reason) {
        return reason.name().toLowerCase(Locale.ROOT);
    }
}
