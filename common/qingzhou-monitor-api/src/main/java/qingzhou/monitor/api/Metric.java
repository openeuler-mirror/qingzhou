package qingzhou.monitor.api;

import java.util.Collections;
import java.util.Map;

/**
 * 单个监控指标样本；name 不含 qingzhou_ 前缀，前缀由端点渲染时单点补齐。
 */
public interface Metric {
    enum Type {
        GAUGE, COUNTER
    }

    String name();

    double value();

    Type type();

    /**
     * 维度集合；key 须匹配 [a-zA-Z_][a-zA-Z0-9_]* 且不得以 __ 开头，
     * 否则端点渲染时抛 IllegalArgumentException，避免污染整段 exposition。
     */
    Map<String, String> labels();


    /**
     * 无维度样本
     */
    static Metric of(String name, double value, Type type) {
        return of(name, value, type, Collections.emptyMap());
    }

    /**
     * 带维度样本；provider 标签由端点注入，同名标签以端点值为准。
     */
    static Metric of(String name, double value, Type type, Map<String, String> labels) {
        return new Metric() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public double value() {
                return value;
            }

            @Override
            public Type type() {
                return type;
            }

            @Override
            public Map<String, String> labels() {
                return labels;
            }
        };
    }
}
