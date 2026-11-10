package qingzhou.monitor;

import java.util.Collections;
import java.util.Map;

/**
 * 单个监控指标样本。name 为未加 qingzhou_ 前缀的原始名：前缀由端点渲染时单点补齐，
 * 提供方与采集器都不得自行拼接，避免同一指标出现双重前缀。
 * 提供方只需用 {@link #of} 产出样本；改名、合并标签由端点侧完成，不在此暴露。
 */
public interface Metric {
    enum Type {
        GAUGE, COUNTER
    }

    /**
     * 无维度样本
     */
    static Metric of(String name, double value, Type type) {
        return of(name, value, type, Collections.emptyMap());
    }

    /**
     * 带维度样本。标签名不得占用 instance/job/qingzhou_instance/app/model/field/provider，
     * 端点内置标签优先，占用者整条样本被丢弃并计入 dropped_samples_total。
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

    String name();

    double value();

    Type type();

    Map<String, String> labels();
}
