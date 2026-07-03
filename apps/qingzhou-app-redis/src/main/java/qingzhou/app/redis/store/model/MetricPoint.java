package qingzhou.app.redis.store.model;

import java.io.Serializable;

public class MetricPoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private long timestamp;
    private double value;

    public MetricPoint() {
    }

    public MetricPoint(long timestamp, double value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
