package qingzhou.api.type;

public interface Dashboard {
    String ACTION_DASHBOARD = "dashboard";

    void dashboardData(String id, DataBuilder builder);

    interface DataBuilder {
        <T> T build(Class<? extends DashboardData> dataType);

        void add(DashboardData dashboardData);
    }

    interface DashboardData {
        DashboardData title(String title);

        DashboardData info(String info);
    }

    interface Basic extends DashboardData {
        Basic put(String key, String value);
    }

    interface Gauge extends DashboardData {
        Gauge fields(String[] fields);

        Gauge addData(String[] data);

        Gauge unit(String unit);

        Gauge statusKey(String statusKey);

        Gauge maxKey(String maxKey);
    }

    interface Histogram extends Gauge {
    }

    interface ShareDataset extends Basic {
    }
}
