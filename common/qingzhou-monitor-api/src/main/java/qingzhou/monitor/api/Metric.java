package qingzhou.monitor.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个监控指标样本：进程内由 of(...) 构造，跨实例时作为 JSON 报文元素收发。
 * name 不含 qingzhou_ 前缀，前缀由端点渲染时单点补齐。
 */
public final class Metric {
    public enum Type {
        GAUGE, COUNTER
    }

    private String name;
    private double value;
    private Type type;
    private String description;
    private Map<String, String> labels;

    // 契约适配：Json 只有 Class<T> 重载且 Jackson 注解包未导出，只能按 JavaBean 约定反序列化
    public Metric() {
    }

    private Metric(String name, double value, Type type, Map<String, String> labels, String description) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.labels = new LinkedHashMap<>(labels);
        this.description = description;
    }

    /**
     * 无维度、无说明样本
     */
    public static Metric of(String name, double value, Type type) {
        return of(name, value, type, Collections.emptyMap(), "");
    }

    /**
     * 无维度样本
     */
    public static Metric of(String name, double value, Type type, String description) {
        return of(name, value, type, Collections.emptyMap(), description);
    }

    /**
     * 带维度、无说明样本；provider 标签由端点注入，同名标签以端点值为准。
     */
    public static Metric of(String name, double value, Type type, Map<String, String> labels) {
        return of(name, value, type, labels, "");
    }

    /**
     * 带维度样本
     */
    public static Metric of(String name, double value, Type type, Map<String, String> labels, String description) {
        return new Metric(name, value, type, labels, description);
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }

    /**
     * 指标语义说明，供端点渲染为人类可读文档；属指标族级属性，同名样本须一致，空串表示不提供说明。
     */
    public String getDescription() {
        return description;
    }

    /**
     * 维度集合；key 须匹配 [a-zA-Z_][a-zA-Z0-9_]* 且不得以 __ 开头，
     * 否则端点渲染时抛 IllegalArgumentException，避免污染整段 exposition。
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    // 契约适配：setter 仅供反序列化使用，进程内一律经 of(...) 构造；不实现 equals/hashCode，
    // 保持 HashSet 按引用判重，重复序列由渲染边界显式拦截
    public void setName(String name) {
        this.name = name;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }
}
