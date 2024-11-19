package qingzhou.core;

import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.api.type.Chart;

public class ChartDataBuilder extends ResponseData implements Chart.DataBuilder {
    public Map<String, String[]> data = new LinkedHashMap<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
    public String[] xAxis; // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

    @Override
    public void setXAxis(String[] xAxis) {
        this.xAxis = xAxis;
    }

    @Override
    public void addLineData(String lineName, String[] lineData) {
        data.put(lineName, lineData);
    }
}
