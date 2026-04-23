package qingzhou.app.demo;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Show;
import qingzhou.api.type.Monitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Dashboard 首页模型
 * 提供实时统计数据展示
 */
@Model(code = "dashboard", order = 1,
        name = {"首页", "en:Dashboard"},
        info = {"数据概览", "en:Dashboard Overview"},
        icon = "HomeFilled")
public class Dashboard extends qingzhou.api.ModelBase implements Show, Monitor {

    @ModelField(id = true,
            name = {"编号", "en:ID"},
            field_type = FieldType.MONITORING,
            show = true)
    public String id;

    @ModelField(
            name = {"统计时间", "en:Stats Time"},
            info = {"数据统计时间", "en:Statistics generation time"},
            field_type = FieldType.MONITORING,
            show = true)
    public String statsTime;

    @ModelField(
            name = {"学生数", "en:Student Count"},
            info = {"总学生数量", "en:Total student count"},
            field_type = FieldType.MONITORING)
    public String studentCount;

    @ModelField(
            name = {"产品数", "en:Product Count"},
            info = {"总产品数量", "en:Total product count"},
            field_type = FieldType.MONITORING)
    public String productCount;

    @ModelField(
            name = {"订单数", "en:Order Count"},
            info = {"总订单数量", "en:Total order count"},
            field_type = FieldType.MONITORING)
    public String orderCount;

    @ModelField(
            name = {"销售额", "en:Sales Amount"},
            info = {"总销售额", "en:Total sales amount"},
            field_type = FieldType.MONITORING)
    public String totalSales;

    @Override
    public Map<String, String> monitor(Request request) {
        Map<String, String> stats = new HashMap<>();

        stats.put("studentCount", "1286");
        stats.put("productCount", "328");
        stats.put("orderCount", "5432");
        stats.put("totalSales", "128650.00");

        stats.put("statsTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));

        return stats;
    }

    @Override
    public Map<String, String> show(Request request) {
        return monitor(request);
    }
}
