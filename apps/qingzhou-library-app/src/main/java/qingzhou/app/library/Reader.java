package qingzhou.app.library;

import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.*;
import qingzhou.api.InputType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Model(code = "reader", order = 2,
        name = {"读者管理", "en:Reader Management"},
        info = {"读者信息管理", "en:Reader information management"},
        icon = "Avatar",
        menu = "basic")
public class Reader extends qingzhou.api.ModelBase implements List, Show, Add, Update, Delete {
    public static final Map<String, Map<String, String>> db = new HashMap<>();
    private int idCounter = 1;

    public Reader() {
        Map<String, String> r1 = new HashMap<>();
        r1.put("id", "R001");
        r1.put("name", "张三");
        r1.put("cardNo", "A20260001");
        r1.put("phone", "13800138001");
        r1.put("email", "zhangsan@example.com");
        r1.put("type", "student");
        r1.put("borrowedCount", "0");
        r1.put("maxBorrow", "10");
        r1.put("status", "active");
        r1.put("createdAt", "2026-01-01 09:00:00");
        db.put(r1.get("id"), r1);

        Map<String, String> r2 = new HashMap<>();
        r2.put("id", "R002");
        r2.put("name", "李四");
        r2.put("cardNo", "A20260002");
        r2.put("phone", "13800138002");
        r2.put("email", "lisi@example.com");
        r2.put("type", "teacher");
        r2.put("borrowedCount", "2");
        r2.put("maxBorrow", "20");
        r2.put("status", "active");
        r2.put("createdAt", "2026-02-05 10:30:00");
        db.put(r2.get("id"), r2);

        Map<String, String> r3 = new HashMap<>();
        r3.put("id", "R003");
        r3.put("name", "王五");
        r3.put("cardNo", "A20260003");
        r3.put("phone", "13800138003");
        r3.put("email", "wangwu@example.com");
        r3.put("type", "other");
        r3.put("borrowedCount", "0");
        r3.put("maxBorrow", "5");
        r3.put("status", "suspended");
        r3.put("createdAt", "2026-03-10 14:00:00");
        db.put(r3.get("id"), r3);

        idCounter = 4;
    }

    @ModelField(id = true,
            name = {"读者编号", "en:Reader ID"},
            info = {"读者唯一标识", "en:Reader unique identifier"},
            list = true,
            show = true,
            readonly = true)
    public String id;

    @ModelField(
            name = {"姓名", "en:Name"},
            info = {"读者姓名", "en:Reader name"},
            list = true,
            search = true,
            add = true,
            update = true,
            required = true)
    public String name;

    @ModelField(
            name = {"借书证号", "en:Card No."},
            info = {"借书证编号", "en:Library card number"},
            list = true,
            search = true,
            add = true,
            update = true,
            required = true)
    public String cardNo;

    @ModelField(
            name = {"手机号", "en:Phone"},
            info = {"联系电话", "en:Phone number"},
            list = true,
            add = true,
            update = true)
    public String phone;

    @ModelField(
            name = {"邮箱", "en:Email"},
            info = {"电子邮箱", "en:Email address"},
            add = true,
            update = true)
    public String email;

    @ModelField(
            name = {"读者类型", "en:Type"},
            info = {"读者类型", "en:Reader type"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.select,
            options = {"student", "teacher", "other"})
    public String type = "student";

    @ModelField(
            name = {"当前借阅数", "en:Borrowed"},
            info = {"当前正在借阅的图书数量", "en:Current borrowed books count"},
            list = true,
            readonly = true,
            input_type = InputType.number)
    public Integer borrowedCount;

    @ModelField(
            name = {"最大可借", "en:Max Borrow"},
            info = {"最多可同时借阅的图书数量", "en:Max books allowed"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.number,
            min = 1)
    public Integer maxBorrow;

    @ModelField(
            name = {"状态", "en:Status"},
            info = {"读者状态", "en:Reader status"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.select,
            options = {"active", "suspended", "cancelled"})
    public String status = "active";

    @ModelField(
            name = {"创建时间", "en:Created"},
            info = {"创建时间", "en:Creation time"},
            list = true,
            show = true,
            readonly = true,
            input_type = InputType.datetime)
    public String createdAt;

    public static void increaseBorrowed(String readerId) {
        Map<String, String> reader = db.get(readerId);
        if (reader != null) {
            int borrowed = Integer.parseInt(reader.get("borrowedCount"));
            reader.put("borrowedCount", String.valueOf(borrowed + 1));
        }
    }

    public static void decreaseBorrowed(String readerId) {
        Map<String, String> reader = db.get(readerId);
        if (reader != null) {
            int borrowed = Integer.parseInt(reader.get("borrowedCount"));
            if (borrowed > 0) {
                reader.put("borrowedCount", String.valueOf(borrowed - 1));
            }
        }
    }

    @Override
    public java.util.List<String[]> list(Request request, int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> result = new ArrayList<>();
        java.util.List<Map<String, String>> filtered = new ArrayList<>();

        for (Map<String, String> reader : db.values()) {
            if (matchesQuery(reader, query)) {
                filtered.add(reader);
            }
        }

        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, filtered.size());

        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> r = filtered.get(i);
            String[] data = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                String value = r.get(listFields[j]);
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
        for (Map<String, String> reader : db.values()) {
            if (matchesQuery(reader, query)) {
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
        String newId = "R" + String.format("%03d", idCounter++);
        data.put("id", newId);
        if (!data.containsKey("borrowedCount") || data.get("borrowedCount") == null) {
            data.put("borrowedCount", "0");
        }
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
