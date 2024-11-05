package qingzhou.deployer;

import qingzhou.api.type.Dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardDataBuilder implements Dashboard.DataBuilder {
    private java.util.List<Dashboard.DataType> dataTypes = new ArrayList<>();

    @Override
    public <T> T build(Class<? extends Dashboard.DataType> dataType) {
        if (dataType.equals(Dashboard.Basic.class)) {
            return (T) new BasicImpl();
        }
        if (dataType.equals(Dashboard.Gauge.class)) {
            return (T) new GaugeImpl();
        }
        if (dataType.equals(Dashboard.Histogram.class)) {
            return (T) new HistogramImpl();
        }
        if (dataType.equals(Dashboard.ShareDataset.class)) {
            return (T) new ShareDatasetImpl();
        }

        return null;
    }

    @Override
    public void add(Dashboard.DataType dataType) {
        dataTypes.add(dataType);
    }

    public List<Dashboard.DataType> getDataTypes() {
        return dataTypes;
    }

    public static class BasicImpl extends DataTypeImpl implements Dashboard.Basic {
        private Map<String, String> data = new HashMap<>();

        @Override
        public Dashboard.Basic put(String key, String value) {
            data.put(key, value);
            return this;
        }

        public Map<String, String> getData() {
            return data;
        }
    }

    public static class DataTypeImpl implements Dashboard.DataType {
        private String title;
        private String info;

        @Override
        public Dashboard.DataType title(String title) {
            this.title = title;
            return this;
        }

        @Override
        public Dashboard.DataType info(String info) {
            this.info = info;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public String getInfo() {
            return info;
        }
    }

    public static class GaugeImpl extends DataTypeImpl implements Dashboard.Gauge {
        private String[] fields;
        private List<String[]> data = new ArrayList<>();
        private String valueKey;
        private String maxKey;
        private String unit;

        @Override
        public Dashboard.Gauge fields(String[] fields) {
            this.fields = fields;
            return this;
        }

        @Override
        public Dashboard.Gauge addData(String[] data) {
            this.data.add(data);
            return this;
        }

        @Override
        public Dashboard.Gauge unit(String unit) {
            this.unit = unit;
            return this;
        }

        @Override
        public Dashboard.Gauge statusKey(String statusKey) {
            this.valueKey = statusKey;
            return this;
        }

        @Override
        public Dashboard.Gauge maxKey(String maxKey) {
            this.maxKey = maxKey;
            return this;
        }

        public String[] getFields() {
            return fields;
        }

        public List<String[]> getData() {
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

    public static class HistogramImpl extends GaugeImpl implements Dashboard.Histogram {
    }

    public static class ShareDatasetImpl extends BasicImpl implements Dashboard.ShareDataset {
    }
}
