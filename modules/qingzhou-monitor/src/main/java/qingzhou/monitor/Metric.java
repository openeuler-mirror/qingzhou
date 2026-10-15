package qingzhou.monitor;

import java.util.Collections;
import java.util.Map;

/**
 * 单个监控指标快照。
 */
public class Metric {
    private final String name;
    private final double value;
    private final Map<String, String> labels;

    public Metric(String name, double value) {
        this(name, value, null);
    }

    public Metric(String name, double value, Map<String, String> labels) {
        this.name = name;
        this.value = value;
        this.labels = labels == null ? Collections.emptyMap() : labels;
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    public Map<String, String> getLabels() {
        return labels;
    }
}
