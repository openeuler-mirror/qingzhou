package qingzhou.app.demo;

import java.util.HashMap;
import java.util.Map;

import qingzhou.api.InputType;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelField;
import qingzhou.api.action.Add;
import qingzhou.api.action.Delete;
import qingzhou.api.action.Page;
import qingzhou.api.action.Show;
import qingzhou.api.action.Update;

/**
 * 教师管理：演示完整 CRUD 能力；其内存表被 {@code Dashboard} 聚合统计（故声明为 static）。
 */
@Model(code = "teacher", order = 2,
        name = {"教师", "en:Teacher"},
        info = {"教师信息管理，演示完整CRUD功能", "en:Teacher information management"},
        icon = "Postcard",
        menu = "basic")
public class Teacher extends AbstractCrudModel implements Page, Show, Add, Update, Delete {
    private static final Map<String, Map<String, String>> db = new HashMap<>();

    public Teacher() {
        if (!db.isEmpty()) return;

        db.put("T001", sample("T001", "王教授", "男", "教授", "计算机学院", "人工智能", "wang@example.com",
                "13800138001", "15000.00", "2010-09-01", "active", "true", "专注人工智能与机器学习研究", "2026-01-10 08:30:00"));
        db.put("T002", sample("T002", "李副教授", "女", "副教授", "计算机学院", "软件工程", "li@example.com",
                "13800138002", "12000.00", "2015-03-15", "active", "true", "研究方向为软件工程与敏捷开发", "2026-01-15 09:40:00"));
        db.put("T003", sample("T003", "张讲师", "男", "讲师", "信息学院", "数据科学", "zhang@example.com",
                "13800138003", "8000.00", "2020-07-20", "active", "false", "专注数据挖掘与大数据分析", "2026-02-01 10:15:00"));
        idCounter = 4;
    }

    private static Map<String, String> sample(String id, String name, String gender, String title, String dept,
                                              String area, String email, String phone, String salary,
                                              String hire, String status, String advisor, String bio, String created) {
        Map<String, String> t = new HashMap<>();
        t.put("id", id);
        t.put("name", name);
        t.put("gender", gender);
        t.put("title", title);
        t.put("department", dept);
        t.put("researchArea", area);
        t.put("email", email);
        t.put("phone", phone);
        t.put("salary", salary);
        t.put("hireDate", hire);
        t.put("status", status);
        t.put("isAdvisor", advisor);
        t.put("bio", bio);
        t.put("createdAt", created);
        t.put("enabled", "true");
        return t;
    }

    @Override
    protected Map<String, Map<String, String>> data() {
        return db;
    }

    @Override
    protected String genId(Map<String, String> d) {
        String id = "T" + nextSeq();
        d.put("id", id);
        d.put("createdAt", DateKit.now());
        return id;
    }

    @ModelField(id = true,
            name = {"工号", "en:ID"},
            info = {"教师唯一标识", "en:Teacher unique identifier"},
            list = true, show = true, readonly = true)
    public String id;

    @ModelField(name = {"姓名", "en:Name"},
            info = {"教师姓名", "en:Teacher name"},
            list = true, search = true, add = true, update = true, required = true)
    public String name;

    @ModelField(name = {"性别", "en:Gender"},
            info = {"教师性别", "en:Teacher gender"},
            list = true, add = true, update = true, input_type = InputType.radio, options = {"男", "女"})
    public Boolean gender;

    @ModelField(name = {"职称", "en:Title"},
            info = {"教师职称", "en:Professional title"},
            list = true, search = true, add = true, update = true, input_type = InputType.select,
            options = {"教授", "副教授", "讲师", "助教"})
    public String title;

    @ModelField(name = {"学院", "en:Department"},
            info = {"所属学院", "en:Academic department"},
            list = true, search = true, add = true, update = true)
    public String department;

    @ModelField(name = {"研究方向", "en:Research Area"},
            info = {"研究方向", "en:Research area"},
            list = true, add = true, update = true, group = "academic")
    public String researchArea;

    @ModelField(name = {"邮箱", "en:Email"},
            info = {"电子邮箱", "en:E-mail address"},
            list = true, add = true, update = true, email = true, group = "Contact")
    public String email;

    @ModelField(name = {"电话", "en:Phone"},
            info = {"手机号码", "en:Phone number"},
            list = true, add = true, update = true, group = "Contact")
    public String phone;

    @ModelField(name = {"薪资", "en:Salary"},
            info = {"月薪资", "en:Monthly salary"},
            list = true, add = true, update = true, input_type = InputType.decimal, min_value = 0, group = "basic")
    public String salary;

    @ModelField(name = {"入职日期", "en:Hire Date"},
            info = {"入职日期", "en:Hire date"},
            list = true, add = true, update = true, group = "basic")
    public String hireDate;

    @ModelField(name = {"状态", "en:Status"},
            info = {"在职状态", "en:Employment status"},
            list = true, add = true, update = true, input_type = InputType.select,
            options = {"active", "on_leave", "resigned"})
    public String status;

    @ModelField(name = {"是否导师", "en:Is Advisor"},
            info = {"是否为研究生导师", "en:Is graduate advisor"},
            list = true, add = true, update = true, input_type = InputType.bool, group = "academic")
    public Boolean isAdvisor;

    @ModelField(name = {"简介", "en:Bio"},
            info = {"个人简介", "en:Personal biography"},
            input_type = InputType.textarea, add = true, update = true, group = "basic")
    public String bio;

    @ModelField(name = {"创建时间", "en:Created"},
            info = {"记录创建时间", "en:Record creation time"},
            list = true, show = true, readonly = true, input_type = InputType.datetime)
    public String createdAt;

    @ModelField(name = {"启用", "en:Enabled"},
            info = {"是否启用", "en:Is enabled"},
            list = true, add = true, update = true, input_type = InputType.bool)
    public Boolean enabled;
}
