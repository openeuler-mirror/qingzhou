package qingzhou.app.demo;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Show;
import qingzhou.api.type.Monitor;

import java.text.SimpleDateFormat;
import java.util.Date;
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

    // ========== 业务数据统计 ==========

    @ModelField(
            name = {"教师总数", "en:Teacher Count"},
            info = {"系统中教师总数", "en:Total teacher count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.stat,
            group = {"业务数据", "en:Business Data"})
    public String teacherCount;

    @ModelField(
            name = {"学生总数", "en:Student Count"},
            info = {"系统中学生总数", "en:Total student count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.stat,
            group = {"业务数据", "en:Business Data"})
    public String studentCount;

    @ModelField(
            name = {"产品总数", "en:Product Count"},
            info = {"系统中产品总数", "en:Total product count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.stat,
            group = {"业务数据", "en:Business Data"})
    public String productCount;

    @ModelField(
            name = {"订单总数", "en:Order Count"},
            info = {"系统中订单总数", "en:Total order count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.stat,
            group = {"业务数据", "en:Business Data"})
    public String orderCount;

    // ========== 教师性别分布（饼图） ==========

    @ModelField(
            name = {"男教师", "en:Male Teachers"},
            info = {"男性教师数量", "en:Male teacher count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.pie,
            group = {"教师性别分布", "en:Teacher Gender Distribution"})
    public String teacherMale;

    @ModelField(
            name = {"女教师", "en:Female Teachers"},
            info = {"女性教师数量", "en:Female teacher count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.pie,
            group = {"教师性别分布", "en:Teacher Gender Distribution"})
    public String teacherFemale;

    // ========== 教师职称分布（柱状图） ==========

    @ModelField(
            name = {"讲师", "en:Lecturer"},
            info = {"讲师数量", "en:Lecturer count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.bar,
            group = {"教师职称分布", "en:Teacher Title Distribution"})
    public String titleLecturer;

    @ModelField(
            name = {"副教授", "en:Associate Professor"},
            info = {"副教授数量", "en:Associate professor count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.bar,
            group = {"教师职称分布", "en:Teacher Title Distribution"})
    public String titleAssociate;

    @ModelField(
            name = {"教授", "en:Professor"},
            info = {"教授数量", "en:Professor count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.bar,
            group = {"教师职称分布", "en:Teacher Title Distribution"})
    public String titleProfessor;

    // ========== 教师启用状态（饼图） ==========

    @ModelField(
            name = {"已启用", "en:Enabled"},
            info = {"已启用教师数量", "en:Enabled teacher count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.pie,
            group = {"教师启用状态", "en:Teacher Status"})
    public String teacherEnabled;

    @ModelField(
            name = {"已禁用", "en:Disabled"},
            info = {"已禁用教师数量", "en:Disabled teacher count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.pie,
            group = {"教师启用状态", "en:Teacher Status"})
    public String teacherDisabled;

    @Override
    public Map<String, String> monitor(Request request) {
        Map<String, String> stats = new HashMap<>();

        // 统计时间
        stats.put("statsTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        // ========== 统计教师数据 ==========
        Teacher teacher = new Teacher();
        int teacherTotal = teacher.db.size();
        stats.put("teacherCount", String.valueOf(teacherTotal));

        // 性别分布
        int maleCount = 0;
        int femaleCount = 0;
        for (Map<String, String> t : teacher.db.values()) {
            String gender = t.get("gender");
            if ("男".equals(gender)) {
                maleCount++;
            } else {
                femaleCount++;
            }
        }
        stats.put("teacherMale", String.valueOf(maleCount));
        stats.put("teacherFemale", String.valueOf(femaleCount));

        // 职称分布
        int lecturerCount = 0;
        int associateCount = 0;
        int professorCount = 0;
        int enabledCount = 0;
        int disabledCount = 0;
        for (Map<String, String> t : teacher.db.values()) {
            String title = t.get("title");
            if ("讲师".equals(title)) {
                lecturerCount++;
            } else if ("副教授".equals(title)) {
                associateCount++;
            } else if ("教授".equals(title)) {
                professorCount++;
            }

            Boolean enabled = "true".equals(t.get("enabled"));
            if (enabled) {
                enabledCount++;
            } else {
                disabledCount++;
            }
        }
        stats.put("titleLecturer", String.valueOf(lecturerCount));
        stats.put("titleAssociate", String.valueOf(associateCount));
        stats.put("titleProfessor", String.valueOf(professorCount));
        stats.put("teacherEnabled", String.valueOf(enabledCount));
        stats.put("teacherDisabled", String.valueOf(disabledCount));

        // ========== 统计其他实体数据 ==========
        Student student = new Student();
        stats.put("studentCount", String.valueOf(student.db.size()));

        Product product = new Product();
        stats.put("productCount", String.valueOf(product.db.size()));

        Order order = new Order();
        stats.put("orderCount", String.valueOf(order.db.size()));

        return stats;
    }

    @Override
    public Map<String, String> show(Request request) {
        return monitor(request);
    }
}
