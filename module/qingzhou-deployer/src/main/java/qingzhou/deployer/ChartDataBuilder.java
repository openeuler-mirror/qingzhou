package qingzhou.deployer;

import qingzhou.api.type.Chart;

import java.io.Serializable;
import java.util.*;

public class ChartDataBuilder implements Chart.DataBuilder, Serializable {
    public Map<String, List<String>> data = new LinkedHashMap<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动
    public List<String> xValues = new ArrayList<>();

    @Override
    public void addData(String group, String[] values) {
        data.put(group, Arrays.asList(values));
    }

    public void setxValues(List<String> xValues) {
        this.xValues = xValues;
    }

    public void addMap(String xValue, Map<String, String> map) {
        xValues.add(xValue);
        for (String key : map.keySet()) {
            data.computeIfAbsent(key, k -> new ArrayList<>()).add(map.get(key));
        }
    }
}
