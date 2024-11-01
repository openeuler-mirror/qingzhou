package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.api.type.Monitor;
import qingzhou.app.ExampleMain;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.util.Utils;

import java.util.ArrayList;
import java.util.List;

@Model(code = "charts", icon = "line-chart",
        entrance = Monitor.ACTION_MONITOR,
        menu = ExampleMain.MENU_1, order = 10,
        name = {"静态图表", "en:Static Charts"},
        info = {"静态图表", "en:Static Charts."})
public class StaticCharts extends ModelBase {

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


    @ModelAction(
            code = Monitor.ACTION_MONITOR, icon = "line-chart", autoRefresh = false, /*xAxisField = "time",*/
            name = {"监视", "en:Monitor"},
            info = {"获取该组件的运行状态信息，该信息可反映组件的健康情况。",
                    "en:Obtain the operating status information of the component, which can reflect the health of the component."})
    public void monitor(Request request) throws Exception {
        String sql = request.getParameter("sql");
        List<String[]> dataList = ((ResponseImpl) request.getResponse()).getDataList();

        if (Utils.isBlank(sql)) {
            for (int i = 0; i < 10; i++) {
                List<String> list = new ArrayList<>();
                list.add(i + "");       // x轴属性的值要在列表的第一个
                list.add(String.valueOf(-i * i + 1));
                list.add(String.valueOf(-i * i + .5));
                list.add(String.valueOf(-i * i + 5));
                dataList.add(list.toArray(new String[0]));
            }
        } else {
            // 不指定监控属性时，返回值的第一项会做为维度，第一项的第一个值会映射到x轴
            try {
                int j = Integer.parseInt(sql);
                for (int i = 0; i < j; i++) {
                    List<String> list = new ArrayList<>();
                    list.add(i + "");   // x轴属性的值要在列表的第一个
                    for (int k = 0; k < j / 2; k++) {
                        list.add(String.valueOf(-i * i + k));
                    }
                    dataList.add(list.toArray(new String[0]));
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
