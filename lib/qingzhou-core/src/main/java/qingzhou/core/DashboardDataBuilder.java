package qingzhou.core;

import qingzhou.api.type.Dashboard;

import java.io.Serializable;
import java.util.*;

public class DashboardDataBuilder implements Dashboard.DataBuilder, Serializable {
    // 定义仪表盘字段数据标识
    public static final String DASHBOARD_FIELD_INFO = "info";
    public static final String DASHBOARD_FIELD_TITLE = "title";
    // 定义仪表盘字段单位标识
    public static final String DASHBOARD_FIELD_UNIT = "unit";
    // 定义仪表盘字段字段标识，值类型为String[]
    public static final String DASHBOARD_FIELD_FIELDS = "fields";
    // 定义仪表盘字段数据标识，值类型为List<String[]>
    public static final String DASHBOARD_FIELD_DATA = "data";
    // 定义仪表盘字段使用量标识，前端返回值使用
    public static final String DASHBOARD_FIELD_USED = "used";
    // 定义仪表盘字段最大值标识，前端返回值使用
    public static final String DASHBOARD_FIELD_MAX = "max";
    public static final String DASHBOARD_FIELD_MATRIXDATA = "matrixData";
    public static final String DASHBOARD_FIELD_SHOWVALUE = "showValue";
    public static final String DASHBOARD_FIELD_XAXIS = "xAxis";
    public static final String DASHBOARD_FIELD_YAXIS = "yAxis";
    public static final String DASHBOARD_FIELD_XAXIS_NAME = "xAxisName";
    public static final String DASHBOARD_FIELD_YAXIS_NAME = "yAxisName";
    // 定义仪表盘图表的键值数据标识，前端返回值使用
    public static final String DASHBOARD_FIELD_BASIC_DATA = Basic.class.getSimpleName();
    // 定义仪表盘图表的仪表盘数据标识，前端返回值使用
    public static final String DASHBOARD_FIELD_GAUGE_DATA = Gauge.class.getSimpleName();
    // 定义仪表盘图表的直方图数据标识，前端返回值使用
    public static final String DASHBOARD_FIELD_HISTOGRAM_DATA = Histogram.class.getSimpleName();
    // 定义仪表盘图表的共享数据集数据标识，前端返回值使用
    public static final String DASHBOARD_FIELD_SHARE_DATASET_DATA = ShareDataset.class.getSimpleName();
    public static final String DASHBOARD_FIELD_HEATMAP_DATA = MatrixHeatmap.class.getSimpleName();
    public static final String DASHBOARD_FIELD_LINE_CHART_DATA = LineChart.class.getSimpleName();

    public List<Dashboard.DashboardData[]> data = new LinkedList<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

    public int period = 2000;

    public void setPeriod(int period) {
        this.period = period;
    }

    public void transformData() {
        for (Dashboard.DashboardData[] dashboardData : data) {
            for (Dashboard.DashboardData dashboard : dashboardData) {
                if (dashboard instanceof Gauge) {
                    sumUsedMax((Gauge) dashboard);
                }
            }
        }
    }

    private void sumUsedMax(DashboardDataBuilder.Gauge gauge) {
        List<String[]> dataList = gauge.data;
        String[] fields = gauge.fields;
        int usedIndex = -1;
        int maxIndex = -1;
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            if (field.equals(gauge.usedKey)) {
                usedIndex = i;
            } else if (field.equals(gauge.maxKey)) {
                maxIndex = i;
            }
        }
        if (usedIndex == -1) {
            return;
        }

        double totalUsed = 0;
        double totalMax = 0;
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

    @Override
    public <T> T buildData(Class<? extends Dashboard.DashboardData> dataType) {
        if (dataType == Dashboard.Basic.class) return (T) new Basic();
        if (dataType == Dashboard.Gauge.class) return (T) new Gauge();
        if (dataType == Dashboard.Histogram.class) return (T) new Histogram();
        if (dataType == Dashboard.ShareDataset.class) return (T) new ShareDataset();
        if (dataType == Dashboard.MatrixHeatmap.class) return (T) new MatrixHeatmap();
        if (dataType == Dashboard.LineChart.class) return (T) new LineChart();
        throw new IllegalArgumentException();
    }

    @Override
    public void addData(Dashboard.DashboardData[] dashboardData) {
        this.data.add(dashboardData);
    }

    public static abstract class DashboardDataImpl implements Dashboard.DashboardData, Serializable {
        public String title; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String type = this.getClass().getSimpleName();

        @Override
        public Dashboard.DashboardData title(String title) {
            this.title = title;
            return this;
        }
    }

    public static class Basic extends DashboardDataImpl implements Dashboard.Basic {
        public Map<String, String> data = new LinkedHashMap<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

        @Override
        public Dashboard.Basic addData(String key, String value) {
            data.put(key, value);
            return this;
        }
    }

    public static class Gauge extends DashboardDataImpl implements Dashboard.Gauge {
        public String info; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String[] fields; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public List<String[]> data = new LinkedList<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String usedKey; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String maxKey; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String unit; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String max; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
        public String used; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

        @Override
        public Dashboard.Gauge info(String info) {
            this.info = info;
            return this;
        }

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
        public Dashboard.Gauge usedKey(String usedKey) {
            this.usedKey = usedKey;
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
        public List<String[]> data = new LinkedList<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

        @Override
        public Dashboard.Basic addData(String key, String value) {
            data.add(new String[]{key, value});
            return this;
        }
    }

    public static class MatrixHeatmap extends DashboardDataImpl implements Dashboard.MatrixHeatmap {
        public List<String> xAxis = new LinkedList<>();
        public List<String> yAxis = new LinkedList<>();
        public final List<int[]> matrixData = new LinkedList<>();
        public boolean showValue;
        public String xAxisName;
        public String yAxisName;

        @Override
        public Dashboard.MatrixHeatmap addData(String xAxis, String yAxis, int value) {
            int initialXSize = this.xAxis.size() - 1; // 这里-1是因为下边返回的是索引位置
            int initialYSize = this.yAxis.size() - 1;

            int xIndex = insertAndGetIndex(this.xAxis, xAxis);
            int yIndex = insertAndGetIndex(this.yAxis, yAxis);

            // 检查是否已经存在相同的 xIndex 和 yIndex
            if (xIndex < initialXSize && yIndex < initialYSize) {
                for (int[] data : matrixData) {
                    if (data[0] == xIndex && data[1] == yIndex) {
                        // 更新现有的 value
                        data[2] = value;
                        return this;
                    }
                }
            }

            matrixData.add(new int[]{xIndex, yIndex, value});
            return this;
        }

        private int insertAndGetIndex(List<String> list, String value) {
            int index = list.indexOf(value);
            if (index == -1) {
                list.add(value);
                index = list.size() - 1;
            }
            return index;
        }

        @Override
        public Dashboard.MatrixHeatmap showValue(boolean show) {
            showValue = show;
            return this;
        }

        @Override
        public Dashboard.MatrixHeatmap xAxisName(String xAxisName) {
            this.xAxisName = xAxisName;
            return this;
        }

        @Override
        public Dashboard.MatrixHeatmap yAxisName(String yAxisName) {
            this.yAxisName = yAxisName;
            return this;
        }
    }


    public static class LineChart extends DashboardDataImpl implements Dashboard.LineChart {
        public final List<Map<String, String>> data = new LinkedList<>();
        public String xAxis;
        public String yAxis;
        public String unit;

        @Override
        public Dashboard.LineChart addData(String name, String value) {
            data.add(new HashMap<String, String>() {{
                put("name", name);
                put("value", value);
            }});
            return this;
        }

        @Override
        public Dashboard.LineChart xAxis(String xAxis) {
            this.xAxis = xAxis;
            return this;
        }

        @Override
        public Dashboard.LineChart yAxis(String yAxis) {
            this.yAxis = yAxis;
            return this;
        }

        @Override
        public Dashboard.LineChart unit(String unit) {
            this.unit = unit;
            return this;
        }
    }
}
