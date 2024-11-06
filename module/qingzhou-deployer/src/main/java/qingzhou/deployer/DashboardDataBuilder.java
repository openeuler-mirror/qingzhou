package qingzhou.deployer;

import qingzhou.api.type.Dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DashboardDataBuilder implements Dashboard.DataBuilder, Serializable {
    private final java.util.List<Dashboard.DashboardData> dataTypes = new ArrayList<>();

    @Override
    public <T> T build(Class<? extends Dashboard.DashboardData> dataType) {
        if (dataType == Dashboard.Basic.class) return (T) new BasicImpl();
        if (dataType == Dashboard.Gauge.class) return (T) new GaugeImpl();
        if (dataType == Dashboard.Histogram.class) return (T) new HistogramImpl();
        if (dataType == Dashboard.ShareDataset.class) return (T) new ShareDatasetImpl();
        throw new IllegalArgumentException();
    }

    @Override
    public void add(Dashboard.DashboardData dashboardData) {
        this.dataTypes.add(dashboardData);
    }

    public List<Dashboard.DashboardData> getDataTypes() {
        return dataTypes;
    }

    public static abstract class DashboardDataImpl implements Dashboard.DashboardData, Serializable {
        private String title;
        private String info;

        @Override
        public Dashboard.DashboardData title(String title) {
            this.title = title;
            return this;
        }

        @Override
        public Dashboard.DashboardData info(String info) {
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

    public static class BasicImpl extends DashboardDataImpl implements Dashboard.Basic {
        private final Map<String, String> data = new LinkedHashMap<>();

        @Override
        public Dashboard.Basic put(String key, String value) {
            data.put(key, value);
            return this;
        }

        public Map<String, String> getData() {
            return data;
        }
    }

    public static class GaugeImpl extends DashboardDataImpl implements Dashboard.Gauge {
        private String[] fields;
        private final List<String[]> data = new LinkedList<>();
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
