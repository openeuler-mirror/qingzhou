package qingzhou.app.model;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Chart;
import qingzhou.app.ExampleMain;
import qingzhou.engine.util.Utils;

@Model(code = "charts", icon = "line-chart",
        entrance = Chart.ACTION_CHART,
        menu = ExampleMain.MENU_1, order = 5,
        name = {"静态图表", "en:Static Charts"},
        info = {"静态图表", "en:Static Charts."})
public class StaticCharts extends ModelBase implements Chart {

    @ModelField(
            name = {"时间", "en:Time"},
            info = {"。", "en:."})
    public int time;

    /*@ModelField(
            field_type = FieldType.MONITOR, numeric = true,
            name = {"A", "en:A"},
            info = {"A info。", "en:A info."})
    public int a;

    @ModelField(
            field_type = FieldType.MONITOR, numeric = true,
            name = {"B", "en:B"},
            info = {"B info。", "en:B info."})
    public int b;

    @ModelField(
            field_type = FieldType.MONITOR, numeric = true, list = true,
            name = {"C", "en:C"},
            info = {"C info。", "en:C info."})
    public int c;*/

    @ModelField(
            search = true,
            input_type = InputType.textarea,
            name = {"sql", "en:sql"},
            info = {"查询语句。", "en:sql."})
    public int sql;

    @Override
    public void chartData(DataBuilder dataBuilder) throws Exception {
        Request request = getAppContext().getCurrentRequest();
        String sql = request.getParameter("sql");

        if (Utils.isBlank(sql)) {
            // 第一种设置数据方式：addMap 每次设置x轴上的多个维度
            for (int i = 0; i < 10; i++) {
                Map<String, String> map = new HashMap<>();
                map.put("a", String.valueOf(-i * i + ThreadLocalRandom.current().nextInt(20)));
                map.put("b", String.valueOf(-i * i + ThreadLocalRandom.current().nextInt(20)));
                map.put("c", String.valueOf(-i * i + ThreadLocalRandom.current().nextInt(20)));
                dataBuilder.addMap(i + "", map);
            }
        } else {
            // 第二种设置数据方式：addData + setxValues 每次设置一个维度上的一组值
            try {
                int j = Integer.parseInt(sql);
                List<String> xValues = new ArrayList<>();
                for (int i = 0; i < j; i++) {
                    xValues.add(i + "");

                }
                dataBuilder.setxValues(xValues);    // 设置x轴数据

                for (int k = 0; k < j / 2; k++) {
                    String group = String.valueOf((char) ('a' + k));
                    List<String> list = new ArrayList<>();
                    for (int i = 0; i < xValues.size(); i++) {
                        list.add(String.valueOf(-i * k + ThreadLocalRandom.current().nextInt(20)));
                    }
                    dataBuilder.addData(group, list.toArray(new String[0]));    // 设置每个维度的数据
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
