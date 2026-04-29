package qingzhou.app.demo;

import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Add;
import qingzhou.api.type.Delete;
import qingzhou.api.type.List;
import qingzhou.api.type.Show;
import qingzhou.api.type.Update;
import qingzhou.api.InputType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Model(code = "teacher", order = 2,
        name = {"教师", "en:Teacher"},
        info = {"教师信息管理，演示完整CRUD功能", "en:Teacher information management"},
        icon = "UserAdd",
        menu = "basic")
public class Teacher extends qingzhou.api.ModelBase implements List, Show, Add, Update, Delete {
    public static final Map<String, Map<String, String>> db = new HashMap<>();
    private int idCounter = 1;

    public Teacher() {
        Map<String, String> t1 = new HashMap<>();
        t1.put("id", "T001");
        t1.put("name", "王教授");
        t1.put("gender", "男");
        t1.put("title", "教授");
        t1.put("department", "计算机学院");
        t1.put("researchArea", "人工智能");
        t1.put("email", "wang@example.com");
        t1.put("phone", "13800138001");
        t1.put("salary", "15000.00");
        t1.put("hireDate", "2010-09-01");
        t1.put("status", "active");
        t1.put("isAdvisor", "true");
        t1.put("bio", "专注人工智能与机器学习研究");
        t1.put("createdAt", "2026-01-10 08:30:00");
        t1.put("enabled", "true");
        db.put(t1.get("id"), t1);

        Map<String, String> t2 = new HashMap<>();
        t2.put("id", "T002");
        t2.put("name", "李副教授");
        t2.put("gender", "女");
        t2.put("title", "副教授");
        t2.put("department", "计算机学院");
        t2.put("researchArea", "软件工程");
        t2.put("email", "li@example.com");
        t2.put("phone", "13800138002");
        t2.put("salary", "12000.00");
        t2.put("hireDate", "2015-03-15");
        t2.put("status", "active");
        t2.put("isAdvisor", "true");
        t2.put("bio", "研究方向为软件工程与敏捷开发");
        t2.put("createdAt", "2026-01-15 09:40:00");
        t2.put("enabled", "true");
        db.put(t2.get("id"), t2);

        Map<String, String> t3 = new HashMap<>();
        t3.put("id", "T003");
        t3.put("name", "张讲师");
        t3.put("gender", "男");
        t3.put("title", "讲师");
        t3.put("department", "信息学院");
        t3.put("researchArea", "数据科学");
        t3.put("email", "zhang@example.com");
        t3.put("phone", "13800138003");
        t3.put("salary", "8000.00");
        t3.put("hireDate", "2020-07-20");
        t3.put("status", "active");
        t3.put("isAdvisor", "false");
        t3.put("bio", "专注数据挖掘与大数据分析");
        t3.put("createdAt", "2026-02-01 10:15:00");
        t3.put("enabled", "true");
        db.put(t3.get("id"), t3);

        idCounter = 4;
    }

    @ModelField(id = true,
            name = {"工号", "en:ID"},
            info = {"教师唯一标识", "en:Teacher unique identifier"},
            list = true,
            show = true,
            readonly = true)
    public String id;

    @ModelField(
            name = {"姓名", "en:Name"},
            info = {"教师姓名", "en:Teacher name"},
            list = true,
            search = true,
            add = true,
            update = true,
            required = true)
    public String name;

    @ModelField(
            name = {"性别", "en:Gender"},
            info = {"教师性别", "en:Teacher gender"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.radio,
            options = {"男", "女"})
    public Boolean gender;

    @ModelField(
            name = {"职称", "en:Title"},
            info = {"教师职称", "en:Professional title"},
            list = true,
            search = true,
            add = true,
            update = true,
            input_type = InputType.select,
            options = {"教授", "副教授", "讲师", "助教"})
    public String title;

    @ModelField(
            name = {"学院", "en:Department"},
            info = {"所属学院", "en:Academic department"},
            list = true,
            search = true,
            add = true,
            update = true)
    public String department;

    @ModelField(
            name = {"研究方向", "en:Research Area"},
            info = {"研究方向", "en:Research area"},
            list = true,
            add = true,
            update = true,
            group = {"学术信息", "en:Academic Info"})
    public String researchArea;

    @ModelField(
            name = {"邮箱", "en:Email"},
            info = {"电子邮箱", "en:E-mail address"},
            list = true,
            add = true,
            update = true,
            email = true,
            group = {"联系方式", "en:Contact"})
    public String email;

    @ModelField(
            name = {"电话", "en:Phone"},
            info = {"手机号码", "en:Phone number"},
            list = true,
            add = true,
            update = true,
            group = {"联系方式", "en:Contact"})
    public String phone;

    @ModelField(
            name = {"薪资", "en:Salary"},
            info = {"月薪资", "en:Monthly salary"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.decimal,
            min = 0,
            group = {"基本信息", "en:Basic Info"})
    public String salary;

    @ModelField(
            name = {"入职日期", "en:Hire Date"},
            info = {"入职日期", "en:Hire date"},
            list = true,
            add = true,
            update = true,
            group = {"基本信息", "en:Basic Info"})
    public String hireDate;

    @ModelField(
            name = {"状态", "en:Status"},
            info = {"在职状态", "en:Employment status"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.select,
            options = {"active", "on_leave", "resigned"})
    public String status;

    @ModelField(
            name = {"是否导师", "en:Is Advisor"},
            info = {"是否为研究生导师", "en:Is graduate advisor"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.bool,
            group = {"学术信息", "en:Academic Info"})
    public Boolean isAdvisor;

    @ModelField(
            name = {"简介", "en:Bio"},
            info = {"个人简介", "en:Personal biography"},
            input_type = InputType.textarea,
            add = true,
            update = true,
            group = {"基本信息", "en:Basic Info"})
    public String bio;

    @ModelField(
            name = {"创建时间", "en:Created"},
            info = {"记录创建时间", "en:Record creation time"},
            list = true,
            show = true,
            readonly = true,
            input_type = InputType.datetime)
    public String createdAt;

    @ModelField(
            name = {"启用", "en:Enabled"},
            info = {"是否启用", "en:Is enabled"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.bool)
    public Boolean enabled;

    @Override
    public java.util.List<String[]> list(Request request, int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> result = new ArrayList<>();
        java.util.List<Map<String, String>> filtered = new ArrayList<>();

        for (Map<String, String> teacher : db.values()) {
            if (matchesQuery(teacher, query)) {
                filtered.add(teacher);
            }
        }

        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, filtered.size());

        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> t = filtered.get(i);
            String[] data = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                String value = t.get(listFields[j]);
                data[j] = value != null ? value : "";
            }
            result.add(data);
        }

        return result;
    }

    private boolean matchesQuery(Map<String, String> data, Map<String, String> query) {
        if (query == null || query.isEmpty()) return true;

        for (Map.Entry<String, String> entry : query.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                String dataValue = data.get(key);
                if (dataValue == null || !dataValue.toLowerCase().contains(value.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int totalSize(Map<String, String> query) {
        int count = 0;
        for (Map<String, String> teacher : db.values()) {
            if (matchesQuery(teacher, query)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean contains(String id) {
        return db.containsKey(id);
    }

    @Override
    public Map<String, String> show(Request request) {
        return db.get(request.getId());
    }

    @Override
    public void add(Request request, Map<String, String> data) throws Exception {
        String newId = "T" + String.format("%03d", idCounter++);
        data.put("id", newId);
        data.put("createdAt", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        db.put(newId, new HashMap<>(data));
    }

    @Override
    public void update(Request request, Map<String, String> data) throws Exception {
        String id = request.getId();
        if (db.containsKey(id)) {
            Map<String, String> existing = db.get(id);
            existing.putAll(data);
            existing.put("id", id);
        }
    }

    @Override
    public void delete(String id) throws Exception {
        db.remove(id);
    }
}
