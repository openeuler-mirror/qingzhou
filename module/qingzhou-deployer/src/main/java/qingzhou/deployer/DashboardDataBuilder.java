package qingzhou.deployer;

import qingzhou.api.type.Dashboard;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DashboardDataBuilder implements Dashboard.DataBuilder, Serializable {
    public Map<String, Dashboard.DashboardData> data = new LinkedHashMap<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

    @Override
    public <T> T buildData(Class<? extends Dashboard.DashboardData> dataType) {
        if (dataType == Dashboard.Basic.class) return (T) new Basic();
        if (dataType == Dashboard.Gauge.class) return (T) new Gauge();
        if (dataType == Dashboard.Histogram.class) return (T) new Histogram();
        if (dataType == Dashboard.ShareDataset.class) return (T) new ShareDataset();
        throw new IllegalArgumentException();
    }

    @Override
    public void addData(Dashboard.DashboardData dashboardData) {
        this.data.put(dashboardData.getClass().getSimpleName(), dashboardData);
    }

    public static abstract class DashboardDataImpl implements Dashboard.DashboardData, Serializable {
        public String title; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String info; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

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
    }

    public static class Basic extends DashboardDataImpl implements Dashboard.Basic {
        public Map<String, String> data = new LinkedHashMap<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

        @Override
        public Dashboard.Basic put(String key, String value) {
            data.put(key, value);
            return this;
        }
    }

    public static class Gauge extends DashboardDataImpl implements Dashboard.Gauge {
        public String[] fields; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public List<String[]> data = new LinkedList<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String valueKey; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String maxKey; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String unit; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

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
    }

    public static class Histogram extends Gauge implements Dashboard.Histogram {
    }

    public static class ShareDataset extends Basic implements Dashboard.ShareDataset {
    }
}
