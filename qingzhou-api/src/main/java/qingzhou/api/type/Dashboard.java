package qingzhou.api.type;

public interface Dashboard {
    String ACTION_DASHBOARD = "dashboard";

    void dashboardData(String id, DataBuilder builder);

    interface DataBuilder {
        <T> T build(Class<? extends DataType> dataType);

        void add(DataType dataType);
    }

    interface DataType {
        DataType title(String title);

        DataType info(String info);
    }

    interface Basic extends DataType {
        Basic put(String key, String value);
    }

    interface Gauge extends DataType {
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
