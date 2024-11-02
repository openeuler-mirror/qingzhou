package qingzhou.deployer.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.api.type.chart.ChartDataBuilder;

public class ChartDataBuilderImpl implements ChartDataBuilder {
    private final Map<String, String[]> data = new LinkedHashMap<>();

    @Override
    public void addData(String group, String[] values) {
        data.put(group, values);
    }

    public Map<String, String[]> getData() {
        return data;
    }
}
