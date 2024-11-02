package qingzhou.api.type;

import qingzhou.api.type.chart.ChartDataBuilder;

public interface Chart {
    String ACTION_CHART = "chart";

    void chartData(ChartDataBuilder dataBuilder) throws Exception;
}
