package qingzhou.app.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.*;

@Model(code = "student", order = 1,
        name = {"学生", "en:Student"},
        info = {"学生信息管理，演示完整CRUD功能", "en:Student information management"},
        icon = "User",
        menu = "basic")
public class Student extends qingzhou.api.ModelBase implements List, Show, Add, Update, Delete {
    public static final Map<String, Map<String, String>> db = new HashMap<>();
    private static int idCounter = 1;

    public Student() {
        if (!db.isEmpty()) return;

        String[] names = {"张三", "李四", "王五", "赵六", "孙七", "周八", "吴九", "郑十", "冯十一", "陈十二"};
        String[] genders = {"男", "女"};
        String[] classes = {"计算机一班", "计算机二班", "软件工程一班", "软件工程二班", "人工智能班"};
        String[] statuses = {"active", "active", "active", "inactive", "suspended"};

        for (int i = 1; i <= 30; i++) {
            Map<String, String> s = new HashMap<>();
            String id = "S" + String.format("%03d", i);
            s.put("id", id);
            s.put("name", names[i % names.length] + (i / 10 > 0 ? i : ""));
            s.put("age", String.valueOf(18 + i % 8));
            s.put("gender", genders[i % 2]);
            s.put("className", classes[i % classes.length]);
            s.put("email", "student" + i + "@example.com");
            s.put("password", "123456");
            s.put("bio", "学生" + i + "的简介");
            s.put("hobbies", i % 2 == 0 ? "篮球,音乐,编程" : "绘画,阅读,游戏");
            s.put("status", statuses[i % statuses.length]);
            s.put("avatar", "");
            s.put("createdAt", "2026-" + String.format("%02d", (i % 12) + 1) + "-" + String.format("%02d", (i % 28) + 1) + " 10:00:00");
            s.put("enabled", i % 5 == 0 ? "false" : "true");
            s.put("skills", i % 2 == 0 ? "Java,Python,Vue" : "UI设计,Photoshop");
            s.put("fullName", names[i % names.length]);
            s.put("themeColor", i % 3 == 0 ? "#FF6B6B" : (i % 3 == 1 ? "#4ECDC4" : "#45B7D1"));
            db.put(s.get("id"), s);
        }
        idCounter = 31;
    }

    @ModelField(id = true,
            name = {"学号", "en:ID"},
            info = {"学生唯一标识", "en:Student unique identifier"},
            list = true,
            show = true,
            readonly = true)
    public String id;

    @ModelField(
            name = {"姓名", "en:Name"},
            info = {"学生姓名", "en:Student name"},
            list = true,
            search = true,
            add = true,
            update = true,
            required = true)
    public String name;

    @ModelField(
            name = {"年龄", "en:Age"},
            info = {"学生年龄", "en:Student age"},
            list = true,
            input_type = InputType.number,
            min = 0,
            max = 150)
    public Integer age;

    @ModelField(
            name = {"性别", "en:Gender"},
            info = {"学生性别", "en:Student gender"},
            list = true,
            required = true,
            input_type = InputType.radio,
            options = {"男", "女"})
    public Boolean gender;

    @ModelField(
            name = {"班级", "en:Class"},
            info = {"所属班级", "en:Class name"},
            list = true,
            search = true,
            add = true,
            update = true)
    public String className;

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
            name = {"密码", "en:Password"},
            info = {"登录密码", "en:Login password"},
            input_type = InputType.password,
            add = true,
            update = true,
            group = {"联系方式", "en:Contact"})
    public String password;

    @ModelField(
            name = {"简介", "en:Bio"},
            info = {"个人简介", "en:Personal biography"},
            input_type = InputType.textarea,
            add = true,
            update = true,
            group = {"个人信息", "en:Personal Info"})
    public String bio;

    @ModelField(
            name = {"爱好", "en:Hobbies"},
            info = {"兴趣爱好", "en:Interests and hobbies"},
            input_type = InputType.multiselect,
            add = true,
            update = true,
            separator = ",",
            options = {"篮球", "足球", "音乐", "绘画", "编程", "阅读", "游泳", "游戏"},
            group = {"个人信息", "en:Personal Info"})
    public String hobbies;

    @ModelField(
            name = {"状态", "en:Status"},
            info = {"学生状态", "en:Student status"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.select,
            options = {"active", "inactive", "suspended"})
    public String status = "active";

    @ModelField(
            name = {"停用原因", "en:Suspension Reason"},
            info = {"停用原因说明，仅状态为suspended时显示", "en:Reason for suspension, only visible when status is suspended"},
            input_type = InputType.textarea,
            add = true,
            update = true,
            display = "status==suspended",
            group = {"个人信息", "en:Personal Info"})
    public String suspensionReason;

    @ModelField(
            name = {"头像", "en:Avatar"},
            info = {"头像图片", "en:Avatar image"},
            input_type = InputType.file,
            add = true,
            update = true,
            group = {"个人信息", "en:Personal Info"})
    public String avatar;

    @ModelField(
            name = {"注册时间", "en:Created"},
            info = {"注册时间", "en:Registration time"},
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
    public Boolean enabled = true;

    @ModelField(
            name = {"技能标签", "en:Skills"},
            info = {"技能标签，可拖拽排序", "en:Skill tags, draggable and sortable"},
            input_type = InputType.sortable,
            separator = ",",
            add = true,
            update = true,
            group = {"个人信息", "en:Personal Info"})
    public String skills;

    @ModelField(
            name = {"姓名组合", "en:Full Name"},
            info = {"姓|名", "en:First Name|Last Name"},
            input_type = InputType.combine,
            separator = "|",
            add = true,
            update = true,
            group = {"个人信息", "en:Personal Info"})
    public String fullName;

    @ModelField(
            name = {"主题色", "en:Theme Color"},
            info = {"个人主题颜色", "en:Personal theme color"},
            input_type = InputType.text,
            color = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD"},
            add = true,
            update = true,
            group = {"个人信息", "en:Personal Info"})
    public String themeColor;

    @Override
    public java.util.List<String[]> list(Request request, int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> result = new ArrayList<>();
        java.util.List<Map<String, String>> filtered = new ArrayList<>();

        for (Map<String, String> student : db.values()) {
            if (matchesQuery(student, query)) {
                filtered.add(student);
            }
        }

        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, filtered.size());

        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> s = filtered.get(i);
            String[] data = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                String value = s.get(listFields[j]);
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
        for (Map<String, String> student : db.values()) {
            if (matchesQuery(student, query)) {
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
        String newId = "S" + String.format("%03d", idCounter++);
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
