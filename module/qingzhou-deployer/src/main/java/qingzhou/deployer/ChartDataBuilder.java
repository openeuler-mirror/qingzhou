package qingzhou.deployer;

import qingzhou.api.type.Chart;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChartDataBuilder implements Chart.DataBuilder {
    private final Map<String, String[]> data = new LinkedHashMap<>();

    @Override
    public void addData(String group, String[] values) {
        data.put(group, values);
    }

    public Map<String, String[]> getData() {
        return data;
    }
}