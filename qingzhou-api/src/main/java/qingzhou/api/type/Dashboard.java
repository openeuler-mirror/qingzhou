package qingzhou.api.type;

public interface Dashboard {
    String ACTION_DASHBOARD = "dashboard";

    void dashboardData(String id, DataBuilder builder);

    default int period() {
        return 2000;
    }

    interface DataBuilder {
        <T> T buildData(Class<? extends DashboardData> dataType);

        void addData(DashboardData[] dashboardDataList);
    }

    interface DashboardData {
        DashboardData title(String title);
    }

    interface Basic extends DashboardData {
        Basic addData(String key, String value);
    }

    interface Gauge extends DashboardData {
        Gauge info(String info);

        Gauge fields(String[] fields);

        Gauge addData(String[] data);

        Gauge usedKey(String usedKey);

        Gauge maxKey(String maxKey);

        Gauge unit(String unit);
    }

    interface Histogram extends Gauge {
    }

    interface ShareDataset extends Basic {
    }

    interface MatrixHeatmap extends DashboardData {
        MatrixHeatmap addData(String xAxis, String yAxis, int value);

        MatrixHeatmap showValue(boolean show);

        MatrixHeatmap xAxisName(String xAxisName);

        MatrixHeatmap yAxisName(String yAxisName);
    }

    interface LineChart extends DashboardData {
        LineChart addData(String name, String value);

        LineChart xAxis(String xAxis);

        LineChart yAxis(String yAxis);

        LineChart unit(String unit);
    }
}
