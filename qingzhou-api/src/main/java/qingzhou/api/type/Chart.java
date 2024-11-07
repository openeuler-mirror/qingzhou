package qingzhou.api.type;

import java.util.List;
import java.util.Map;

public interface Chart {
    String ACTION_CHART = "chart";

    void chartData(DataBuilder dataBuilder) throws Exception;

    interface DataBuilder {
        void addData(String group, String[] values);

        void setxValues(List<String> xValues);

        void addMap(String xValue, Map<String, String> map);
    }
}
