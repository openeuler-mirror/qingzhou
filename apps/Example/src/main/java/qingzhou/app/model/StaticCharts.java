package qingzhou.app.model;

import java.util.ArrayList;
import java.util.List;

import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Chart;
import qingzhou.api.type.chart.ChartDataBuilder;
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
    public void chartData(ChartDataBuilder dataBuilder) throws Exception {
        Request request = getAppContext().getCurrentRequest();
        String sql = request.getParameter("sql");

        if (Utils.isBlank(sql)) {
            for (int i = 0; i < 10; i++) {
                List<String> list = new ArrayList<>();
                list.add(String.valueOf(-i * i + 1));
                list.add(String.valueOf(-i * i + .5));
                list.add(String.valueOf(-i * i + 5));
                dataBuilder.addData(i + "", list.toArray(new String[0]));
            }
        } else {
            // 不指定监控属性时，返回值的第一项会做为维度，第一项的第一个值会映射到x轴
            try {
                int j = Integer.parseInt(sql);
                List<String> dimensions = new ArrayList<>();
                for (int k = 0; k < j / 2; k++) {
                    dimensions.add(String.valueOf((char) ('a' + k)));
                }
                dataBuilder.addData("time", dimensions.toArray(new String[0]));
                for (int i = 0; i < j; i++) {
                    List<String> list = new ArrayList<>();
                    for (int k = 0; k < j / 2; k++) {
                        list.add(String.valueOf(-i * i + k));
                    }
                    dataBuilder.addData(i + "", list.toArray(new String[0]));
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
