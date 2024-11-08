package qingzhou.deployer;

import qingzhou.api.type.Dashboard;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DashboardDataBuilder implements Dashboard.DataBuilder, Serializable {
    public java.util.List<Dashboard.DashboardData[]> data = new LinkedList<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

    public void transformData() {
        for (Dashboard.DashboardData[] dashboardData : data) {
            for (Dashboard.DashboardData dashboard : dashboardData) {
                if (dashboard instanceof Gauge) {
                    processGaugeOrHistogram((Gauge) dashboard);
                } else if (dashboard instanceof ShareDataset) {
                    processShareDataset((ShareDataset) dashboard);
                }
            }
        }
    }

    private void processGaugeOrHistogram(DashboardDataBuilder.Gauge gauge) {
        java.util.List<String[]> dataList = gauge.data;
        String[] fields = gauge.fields;
        int usedIndex = -1;
        int maxIndex = -1;
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            if (field.equals(gauge.valueKey)) {
                usedIndex = i;
            } else if (field.equals(gauge.maxKey)) {
                maxIndex = i;
            }
        }
        if (usedIndex == -1) {
            return;
        }

        double totalUsed = 0;
        double totalMax = -1;
        for (String[] dataArray : dataList) {
            if (dataArray == null) continue;
            try {
                totalUsed += Double.parseDouble(dataArray[usedIndex]);
                if (maxIndex != -1) {
                    totalMax += Double.parseDouble(dataArray[maxIndex]);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        gauge.max = String.valueOf(totalMax);
        gauge.used = String.valueOf(totalUsed);
    }

    private void processShareDataset(DashboardDataBuilder.ShareDataset shareDataset) {
        try {
            java.util.List<String[]> dataList = new LinkedList<>();
            Map<String, String> data = shareDataset.sourceData;
            Set<String> keySet = data.keySet();
            for (String key : keySet) {
                java.util.List<String> fieldData = new LinkedList<>();
                fieldData.add(key);
                fieldData.add(data.get(key));
                dataList.add(fieldData.toArray(new String[0]));
            }
            shareDataset.data = dataList;
        } catch (Exception ignored) {
        }
    }

    @Override
    public <T> T buildData(Class<? extends Dashboard.DashboardData> dataType) {
        if (dataType == Dashboard.Basic.class) return (T) new Basic();
        if (dataType == Dashboard.Gauge.class) return (T) new Gauge();
        if (dataType == Dashboard.Histogram.class) return (T) new Histogram();
        if (dataType == Dashboard.ShareDataset.class) return (T) new ShareDataset();
        throw new IllegalArgumentException();
    }

    @Override
    public void addData(Dashboard.DashboardData[] dashboardData) {
        this.data.add(dashboardData);
    }

    public static abstract class DashboardDataImpl implements Dashboard.DashboardData, Serializable {
        public String title; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String info; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String type = this.getClass().getSimpleName();

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
        public String max; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String used; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

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

    public static class ShareDataset extends DashboardDataImpl implements Dashboard.ShareDataset {
        public Map<String, String> sourceData = new LinkedHashMap<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

        public java.util.List<String[]> data = new LinkedList<>();

        @Override
        public Dashboard.Basic put(String key, String value) {
            sourceData.put(key, value);
            return this;
        }
    }
}
