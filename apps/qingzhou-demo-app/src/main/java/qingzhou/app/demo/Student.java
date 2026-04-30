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
    public final Map<String, Map<String, String>> db = new HashMap<>();
    private int idCounter = 1;

    public Student() {
        Map<String, String> s1 = new HashMap<>();
        s1.put("id", "S001");
        s1.put("name", "张三");
        s1.put("age", "20");
        s1.put("gender", "男");
        s1.put("className", "计算机一班");
        s1.put("email", "zhangsan@example.com");
        s1.put("password", "123456");
        s1.put("bio", "热爱编程的学生");
        s1.put("hobbies", "篮球,音乐,编程");
        s1.put("status", "active");
        s1.put("avatar", "");
        s1.put("createdAt", "2026-01-15 10:30:00");
        s1.put("enabled", "true");
        s1.put("skills", "Java,Python,Vue");
        s1.put("fullName", "张 三");
        s1.put("themeColor", "#4ECDC4");
        db.put(s1.get("id"), s1);

        Map<String, String> s2 = new HashMap<>();
        s2.put("id", "S002");
        s2.put("name", "李四");
        s2.put("age", "22");
        s2.put("gender", "女");
        s2.put("className", "计算机二班");
        s2.put("email", "lisi@example.com");
        s2.put("password", "654321");
        s2.put("bio", "喜欢设计的学生");
        s2.put("hobbies", "绘画,音乐");
        s2.put("status", "active");
        s2.put("avatar", "");
        s2.put("createdAt", "2026-02-20 14:20:00");
        s2.put("enabled", "true");
        s2.put("skills", "UI设计,Photoshop");
        s2.put("fullName", "李 四");
        s2.put("themeColor", "#FF6B6B");
        db.put(s2.get("id"), s2);
        idCounter = 3;
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
