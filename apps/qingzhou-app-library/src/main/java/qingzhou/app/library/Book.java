package qingzhou.app.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.*;
import qingzhou.api.type.*;
import qingzhou.jdbc.JdbcPool;
import qingzhou.logger.Logger;

@Model(code = "book", order = 1,
        name = {"图书管理", "en:Book Management"},
        info = {"图书信息管理", "en:Book information management"},
        icon = "Reading",
        menu = "basic")
public class Book extends qingzhou.api.ModelBase implements List, Show, Add, Update, Delete {
    public static final Map<String, Map<String, String>> db = new HashMap<>();
    private static int idCounter = 1;

    public Book() {
        if (!db.isEmpty()) return;

        String[] names = {"深入理解计算机系统", "JavaScript高级程序设计", "百年孤独", "算法导论", "设计模式",
                "编译原理", "数据库系统概念", "操作系统概念", "计算机网络", "人工智能",
                "Python编程", "Java核心技术", "Spring实战", "Vue.js设计与实现", "React进阶",
                "经济学原理", "乌合之众", "人类简史", "活着", "三体",
                "围城", "红楼梦", "时间简史", "万历十五年", "苏菲的世界"};
        String[] authors = {"Randal E. Bryant", "Zakas", "加西亚·马尔克斯", "Cormen", "GoF",
                "Aho", "Silberschatz", "Silberschatz", "Andrew S. Tanenbaum", "Stuart Russell",
                "Eric Matthes", "Cay S. Horstmann", "Craig Walls", "霍春阳", "Dan Abramov",
                "曼昆", "勒庞", "尤瓦尔·赫拉利", "余华", "刘慈欣",
                "钱钟书", "曹雪芹", "霍金", "黄仁宇", "Jostein Gaarder"};
        String[] categories = {"计算机", "计算机", "文学", "计算机", "计算机",
                "计算机", "计算机", "计算机", "计算机", "计算机",
                "计算机", "计算机", "计算机", "计算机", "计算机",
                "经济", "哲学", "历史", "文学", "文学",
                "文学", "文学", "历史", "历史", "哲学"};
        String[] publishers = {"机械工业出版社", "人民邮电出版社", "南海出版公司", "机械工业出版社", "机械工业出版社",
                "机械工业出版社", "高等教育出版社", "高等教育出版社", "电子工业出版社", "清华大学出版社",
                "人民邮电出版社", "机械工业出版社", "人民邮电出版社", "人民邮电出版社", "人民邮电出版社",
                "北京大学出版社", "中央编译出版社", "中信出版社", "作家出版社", "重庆出版社",
                "人民文学出版社", "人民文学出版社", "湖南科学技术出版社", "生活·读书·新知三联书店", "作家出版社"};

        for (int i = 1; i <= 25; i++) {
            Map<String, String> b = new HashMap<>();
            String id = "B" + String.format("%03d", i);
            b.put("id", id);
            b.put("isbn", "978-7-" + String.format("%03d", 100 + i) + "-" + String.format("%05d", i * 1007) + "-" + (i % 9 + 1));
            b.put("name", names[i - 1]);
            b.put("author", authors[i - 1]);
            b.put("publisher", publishers[i - 1]);
            b.put("category", categories[i - 1]);
            b.put("total", String.valueOf(5 + i % 16));
            b.put("available", String.valueOf(i % 10));
            b.put("price", String.format("%.2f", 29.0 + i * 6.8));
            b.put("status", i % 10 == 0 ? "borrowed_all" : (i % 7 == 0 ? "damaged" : "available"));
            b.put("createdAt", "2026-" + String.format("%02d", (i % 12) + 1) + "-" + String.format("%02d", (i * 3 % 28) + 1) + " 09:00:00");
            db.put(b.get("id"), b);
        }
        idCounter = 26;
    }

    @ModelField(id = true,
            name = {"图书编号", "en:Book ID"},
            info = {"图书唯一标识", "en:Book unique identifier"},
            list = true,
            search = true,
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
        Logger logger = getAppContext().getService(Logger.class);
        JdbcPool jdbcPool = getAppContext().getService(JdbcPool.class);
        logger.info("jdbcPool: " + jdbcPool);
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
    public java.util.List<String[]> list(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
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
    public Map<String, String> show(String id) {
        return db.get(id);
    }

    @Override
    public void add(Map<String, String> data) {
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
    public void update(String id, Map<String, String> data) {
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
