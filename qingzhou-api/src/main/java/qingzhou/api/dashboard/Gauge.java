package qingzhou.api.dashboard;

public interface Gauge extends DataType {
    Gauge fields(String[] fields);

    Gauge addData(String[] data);

    Gauge unit(String unit);

    Gauge valueKey(String valueKey);

    Gauge maxKey(String maxKey);
}
