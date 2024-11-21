package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.api.type.Chart;
import qingzhou.app.ExampleMain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Model(code = "charts", icon = "line-chart",
        entrance = Chart.ACTION_CHART,
        menu = ExampleMain.MENU_1, order = "5",
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
            input_type = InputType.number,
            name = {"sql", "en:sql"},
            info = {"查询语句。", "en:sql."})
    public int sql;

    @Override
    public void chartData(DataBuilder dataBuilder) {
        Request request = getAppContext().getCurrentRequest();
        String sql = request.getParameter("sql");
        int j = 10;
        try {
            j = Integer.parseInt(sql);
        } catch (Exception e) {
            System.err.println("参数需要数字类型的");
        }
        List<String> xValues = new ArrayList<>();
        for (int i = 0; i < j; i++) {
            xValues.add(i + "");
        }
        dataBuilder.setXAxis(xValues.toArray(new String[0]));    // 设置x轴数据

        for (int k = 0; k < j / 2; k++) {
            String group = String.valueOf((char) ('a' + k));
            List<String> list = new ArrayList<>();
            for (int i = 0; i < xValues.size(); i++) {
                list.add(String.valueOf(-i * k + ThreadLocalRandom.current().nextInt(20)));
            }
            dataBuilder.addLineData(group, list.toArray(new String[0]));    // 设置每个维度的数据
        }
    }
}
