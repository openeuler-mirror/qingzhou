package qingzhou.app.demo;

import java.util.HashMap;
import java.util.Map;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.Request;
import qingzhou.api.action.Monitor;
import qingzhou.api.action.Show;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelAction;
import qingzhou.api.annotations.ModelField;

/**
 * 首页仪表盘：聚合各业务模型的内存数据生成统计图表（饼图 / 柱状图 / 卡片）。
 *
 * <p>跨模型读取通过 {@code new Xxx().data()} 获取其静态内存表（见各 CRUD 模型）。
 */
@Model(code = "dashboard", order = 1,
        name = {"首页", "en:Dashboard"},
        info = {"数据概览", "en:Dashboard Overview"},
        icon = "HomeFilled")
public class Dashboard extends qingzhou.api.ModelBase implements Show, Monitor {

    @ModelField(id = true, name = {"编号", "en:ID"}, field_type = FieldType.monitor, show = true)
    public String id;

    @ModelField(name = {"统计时间", "en:Stats Time"}, info = {"数据统计时间", "en:Statistics generation time"},
            field_type = FieldType.monitor, show = true)
    public String statsTime;

    // ========== 业务数据统计（卡片） ==========

    @ModelField(name = {"教师总数", "en:Teacher Count"}, info = {"系统中教师总数", "en:Total teacher count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.stat, group = "Business")
    public String teacherCount;

    @ModelField(name = {"学生总数", "en:Student Count"}, info = {"系统中学生总数", "en:Total student count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.stat, group = "Business")
    public String studentCount;

    @ModelField(name = {"产品总数", "en:Product Count"}, info = {"系统中产品总数", "en:Total product count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.stat, group = "Business")
    public String productCount;

    @ModelField(name = {"订单总数", "en:Order Count"}, info = {"系统中订单总数", "en:Total order count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.stat, group = "Business")
    public String orderCount;

    // ========== 教师性别分布（饼图） ==========

    @ModelField(name = {"男教师", "en:Male Teachers"}, info = {"男性教师数量", "en:Male teacher count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.pie, group = "TeacherGender")
    public String teacherMale;

    @ModelField(name = {"女教师", "en:Female Teachers"}, info = {"女性教师数量", "en:Female teacher count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.pie, group = "TeacherGender")
    public String teacherFemale;

    // ========== 教师职称分布（柱状图） ==========

    @ModelField(name = {"讲师", "en:Lecturer"}, info = {"讲师数量", "en:Lecturer count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.bar, group = "TeacherTitle")
    public String titleLecturer;

    @ModelField(name = {"副教授", "en:Associate Professor"}, info = {"副教授数量", "en:Associate professor count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.bar, group = "TeacherTitle")
    public String titleAssociate;

    @ModelField(name = {"教授", "en:Professor"}, info = {"教授数量", "en:Professor count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.bar, group = "TeacherTitle")
    public String titleProfessor;

    // ========== 教师启用状态（饼图） ==========

    @ModelField(name = {"已启用", "en:Enabled"}, info = {"已启用教师数量", "en:Enabled teacher count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.pie, group = "TeacherStatus")
    public String teacherEnabled;

    @ModelField(name = {"已禁用", "en:Disabled"}, info = {"已禁用教师数量", "en:Disabled teacher count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.pie, group = "TeacherStatus")
    public String teacherDisabled;

    @Override
    public Map<String, String> monitor(String id) {
        Map<String, String> stats = new HashMap<>();
        stats.put("statsTime", DateKit.now());

        // 教师：单次遍历统计性别 / 职称 / 启用状态
        Map<String, Map<String, String>> teachers = new Teacher().data();
        stats.put("teacherCount", String.valueOf(teachers.size()));
        int male = 0, female = 0, lecturer = 0, associate = 0, professor = 0, enabled = 0, disabled = 0;
        for (Map<String, String> t : teachers.values()) {
            if ("男".equals(t.get("gender"))) male++;
            else female++;
            switch (t.getOrDefault("title", "")) {
                case "讲师":
                    lecturer++;
                    break;
                case "副教授":
                    associate++;
                    break;
                case "教授":
                    professor++;
                    break;
            }
            if ("true".equals(t.get("enabled"))) enabled++;
            else disabled++;
        }
        stats.put("teacherMale", String.valueOf(male));
        stats.put("teacherFemale", String.valueOf(female));
        stats.put("titleLecturer", String.valueOf(lecturer));
        stats.put("titleAssociate", String.valueOf(associate));
        stats.put("titleProfessor", String.valueOf(professor));
        stats.put("teacherEnabled", String.valueOf(enabled));
        stats.put("teacherDisabled", String.valueOf(disabled));

        // 其余实体：直接取总数
        stats.put("studentCount", String.valueOf(new Student().data().size()));
        stats.put("productCount", String.valueOf(new Product().data().size()));
        stats.put("orderCount", String.valueOf(new Order().data().size()));
        return stats;
    }

    @Override
    public Map<String, String> show(String id) {
        return monitor(id);
    }

    /**
     * 自定义 Action 示例：演示直接操控响应（状态码 / 头 / 内容类型）。
     */
    @ModelAction
    public void custom(Request request) {
        request.getResponse()
                .header("test-header-key", "test-header-val")
                .contentType("text/html;")
                .status(222);
    }
}
