package qingzhou.app.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.*;
import qingzhou.api.type.*;

@Model(code = "book", order = 1,
        name = {"图书管理", "en:Book Management"},
        info = {"图书信息管理", "en:Book information management"},
        icon = "Reading",
        menu = "basic")
public class Book extends qingzhou.api.ModelBase implements List, Show, Add, Update, Delete {
    public static final Map<String, Map<String, String>> db = new HashMap<>();
    private int idCounter = 1;

    public Book() {
        Map<String, String> b1 = new HashMap<>();
        b1.put("id", "B001");
        b1.put("isbn", "978-7-111-21382-6");
        b1.put("name", "深入理解计算机系统");
        b1.put("author", "Randal E. Bryant");
        b1.put("publisher", "机械工业出版社");
        b1.put("category", "计算机");
        b1.put("total", "10");
        b1.put("available", "8");
        b1.put("price", "139.00");
        b1.put("status", "available");
        b1.put("createdAt", "2026-01-05 09:00:00");
        db.put(b1.get("id"), b1);

        Map<String, String> b2 = new HashMap<>();
        b2.put("id", "B002");
        b2.put("isbn", "978-7-115-42802-8");
        b2.put("name", "JavaScript高级程序设计");
        b2.put("author", "Zakas");
        b2.put("publisher", "人民邮电出版社");
        b2.put("category", "计算机");
        b2.put("total", "15");
        b2.put("available", "12");
        b2.put("price", "129.00");
        b2.put("status", "available");
        b2.put("createdAt", "2026-02-10 10:30:00");
        db.put(b2.get("id"), b2);

        Map<String, String> b3 = new HashMap<>();
        b3.put("id", "B003");
        b3.put("isbn", "978-7-5442-6996-2");
        b3.put("name", "百年孤独");
        b3.put("author", "加西亚·马尔克斯");
        b3.put("publisher", "南海出版公司");
        b3.put("category", "文学");
        b3.put("total", "20");
        b3.put("available", "0");
        b3.put("price", "39.50");
        b3.put("status", "borrowed_all");
        b3.put("createdAt", "2026-03-01 14:00:00");
        db.put(b3.get("id"), b3);

        idCounter = 4;
    }

    @ModelField(id = true,
            name = {"图书编号", "en:Book ID"},
            info = {"图书唯一标识", "en:Book unique identifier"},
            list = true,
            show = true,
            readonly = true)
    public String id;

    @ModelField(
            name = {"ISBN", "en:ISBN"},
            info = {"国际标准书号", "en:International Standard Book Number"},
            list = true,
            show = true,
            search = true,
            add = true,
            update = true)
    public String isbn;

    @ModelField(
            name = {"书名", "en:Book Name"},
            info = {"图书名称", "en:Book name"},
            list = true,
            search = true,
            add = true,
            update = true,
            required = true)
    public String name;

    @ModelField(
            name = {"作者", "en:Author"},
            info = {"图书作者", "en:Book author"},
            list = true,
            add = true,
            update = true)
    public String author;

    @ModelField(
            name = {"出版社", "en:Publisher"},
            info = {"图书出版社", "en:Book publisher"},
            list = true,
            add = true,
            update = true)
    public String publisher;

    @ModelField(
            name = {"分类", "en:Category"},
            info = {"图书分类", "en:Book category"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.select,
            options = {"计算机", "文学", "历史", "哲学", "经济", "其他"})
    public String category = "其他";

    @ModelField(
            name = {"总数量", "en:Total Copies"},
            info = {"图书总册数", "en:Total book copies"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.number,
            min = 1)
    public Integer total;

    @ModelField(
            name = {"可借数量", "en:Available Copies"},
            info = {"当前可借图书数量", "en:Available book copies"},
            list = true,
            readonly = true,
            input_type = InputType.number)
    public Integer available;

    @ModelField(
            name = {"价格", "en:Price"},
            info = {"图书定价", "en:Book price"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.decimal,
            min = 0)
    public String price;

    @ModelField(
            name = {"状态", "en:Status"},
            info = {"图书状态", "en:Book status"},
            list = true,
            readonly = true,
            input_type = InputType.select,
            options = {"available", "borrowed_all", "damaged"})
    public String status = "available";

    @ModelField(
            name = {"创建时间", "en:Created"},
            info = {"创建时间", "en:Creation time"},
            list = true,
            show = true,
            readonly = true,
            input_type = InputType.datetime)
    public String createdAt;

    private SharedFunctionRegistration registration;

    @Override
    public void start() {
        registration = getAppContext().registerSharedFunction("queryBook", new SharedFunction<String, String>() {
            @Override
            public String invoke(String o) {
                return "From 图书管理：" + o + "库存 " + db.size();
            }
        });
    }

    @Override
    public void stop() {
        registration.unregister();
    }

    @ModelAction(code = "addStock", icon = "Plus",
            name = {"增加库存", "en:Add Stock"},
            info = {"将该图书的可借数量+1", "en:Increase available copies by 1"})
    public void addStock(Request request) {
        String id = request.getId();
        Map<String, String> book = db.get(id);
        if (book != null) {
            int total = Integer.parseInt(book.get("total"));
            int available = Integer.parseInt(book.get("available"));
            book.put("total", String.valueOf(total + 1));
            book.put("available", String.valueOf(available + 1));
            updateBookStatusStatic(book);
        }
    }

    private static void updateBookStatusStatic(Map<String, String> book) {
        int total = Integer.parseInt(book.get("total"));
        int available = Integer.parseInt(book.get("available"));
        if (available == 0) {
            book.put("status", "borrowed_all");
        } else if (available == total) {
            book.put("status", "available");
        }
    }

    public static void decreaseAvailable(String bookId) {
        Map<String, String> book = db.get(bookId);
        if (book != null) {
            int available = Integer.parseInt(book.get("available"));
            if (available > 0) {
                book.put("available", String.valueOf(available - 1));
                updateBookStatusStatic(book);
            }
        }
    }

    public static void increaseAvailable(String bookId) {
        Map<String, String> book = db.get(bookId);
        if (book != null) {
            int total = Integer.parseInt(book.get("total"));
            int available = Integer.parseInt(book.get("available"));
            if (available < total) {
                book.put("available", String.valueOf(available + 1));
                updateBookStatusStatic(book);
            }
        }
    }

    @Override
    public java.util.List<String[]> list(Request request, int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> result = new ArrayList<>();
        java.util.List<Map<String, String>> filtered = new ArrayList<>();

        for (Map<String, String> book : db.values()) {
            if (matchesQuery(book, query)) {
                filtered.add(book);
            }
        }

        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, filtered.size());

        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> b = filtered.get(i);
            String[] data = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                String value = b.get(listFields[j]);
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
        for (Map<String, String> book : db.values()) {
            if (matchesQuery(book, query)) {
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
        String newId = "B" + String.format("%03d", idCounter++);
        data.put("id", newId);
        if (!data.containsKey("available") || data.get("available") == null) {
            data.put("available", data.get("total"));
        }
        data.put("status", "available");
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
            updateBookStatusStatic(existing);
        }
    }

    @Override
    public void delete(String id) throws Exception {
        db.remove(id);
    }
}
