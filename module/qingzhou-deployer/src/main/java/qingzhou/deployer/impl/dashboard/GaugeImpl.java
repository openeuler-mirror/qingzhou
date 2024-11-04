package qingzhou.deployer.impl.dashboard;

import qingzhou.api.dashboard.Gauge;

import java.util.ArrayList;

public class GaugeImpl extends DataTypeImpl implements Gauge {
    private String[] fields;
    private java.util.List<String[]> data = new ArrayList<>();
    private String valueKey;
    private String maxKey;
    private String unit;

    @Override
    public Gauge fields(String[] fields) {
        this.fields = fields;
        return this;
    }

    @Override
    public Gauge addData(String[] data) {
        this.data.add(data);
        return this;
    }

    @Override
    public Gauge unit(String unit) {
        this.unit = unit;
        return this;
    }

    @Override
    public Gauge valueKey(String valueKey) {
        this.valueKey = valueKey;
        return this;
    }

    @Override
    public Gauge maxKey(String maxKey) {
        this.maxKey = maxKey;
        return this;
    }

    public String[] getFields() {
        return fields;
    }

    public java.util.List<String[]> getData() {
        return data;
    }

    public String getValueKey() {
        return valueKey;
    }

    public String getMaxKey() {
        return maxKey;
    }

    public String getUnit() {
        return unit;
    }
}
