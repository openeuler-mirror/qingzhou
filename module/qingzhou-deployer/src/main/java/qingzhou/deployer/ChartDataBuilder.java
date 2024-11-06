package qingzhou.deployer;

import qingzhou.api.type.Chart;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChartDataBuilder implements Chart.DataBuilder, Serializable {
    public Map<String, String[]> data = new LinkedHashMap<>(); // public 是为了凸显 该字段会映射为 json 的 key，最好不要变动

    @Override
    public void addData(String group, String[] values) {
        data.put(group, values);
    }
}
