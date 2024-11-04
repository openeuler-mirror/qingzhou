package qingzhou.api.type;

public interface Chart {
    String ACTION_CHART = "chart";

    void chartData(DataBuilder dataBuilder) throws Exception;

    interface DataBuilder {
        void addData(String group, String[] values);
    }
}
