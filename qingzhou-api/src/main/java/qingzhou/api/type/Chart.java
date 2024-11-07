package qingzhou.api.type;

public interface Chart {
    String ACTION_CHART = "chart";

    void chartData(DataBuilder dataBuilder) throws Exception;

    interface DataBuilder {
        void setXAxis(String[] xAxis);

        void addLineData(String lineName, String[] lineData);
    }
}
