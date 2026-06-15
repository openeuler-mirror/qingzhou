package qingzhou.app.library;

import java.text.SimpleDateFormat;
import java.util.*;

import qingzhou.api.*;
import qingzhou.api.type.Add;
import qingzhou.api.type.Delete;
import qingzhou.api.type.List;
import qingzhou.api.type.Show;

@Model(code = "borrow", order = 1,
        name = {"借阅记录", "en:Borrow Records"},
        info = {"图书借阅与归还管理", "en:Book borrow and return management"},
        icon = "Operation",
        menu = "borrow")
public class Borrow extends qingzhou.api.ModelBase implements List, Show, Add, Delete {
    public static final Map<String, Map<String, String>> db = new HashMap<>();
    private static int idCounter = 3;

    public Borrow() {
        if (!db.isEmpty()) return;

        Map<String, String> br1 = new HashMap<>();
        br1.put("id", "BR001");
        br1.put("readerId", "R002");
        br1.put("readerName", "李四");
        br1.put("bookId", "B001");
        br1.put("bookName", "深入理解计算机系统");
        br1.put("borrowDate", "2026-04-01 09:00:00");
        br1.put("dueDate", "2026-04-30 23:59:59");
        br1.put("returnDate", "");
        br1.put("status", "borrowing");
        br1.put("remark", "");
        db.put(br1.get("id"), br1);

        Map<String, String> br2 = new HashMap<>();
        br2.put("id", "BR002");
        br2.put("readerId", "R002");
        br2.put("readerName", "李四");
        br2.put("bookId", "B002");
        br2.put("bookName", "JavaScript高级程序设计");
        br2.put("borrowDate", "2026-04-10 14:30:00");
        br2.put("dueDate", "2026-05-10 23:59:59");
        br2.put("returnDate", "");
        br2.put("status", "borrowing");
        br2.put("remark", "");
        db.put(br2.get("id"), br2);

        idCounter = 3;
    }

    @ModelField(id = true,
            name = {"记录编号", "en:Record ID"},
            info = {"借阅记录唯一标识", "en:Borrow record unique identifier"},
            list = true,
            show = true,
            readonly = true)
    public String id;

    @ModelField(
            name = {"读者编号", "en:Reader ID"},
            info = {"读者编号", "en:Reader ID"},
            list = true,
            add = true,
            required = true)
    public String readerId;

    @ModelField(
            name = {"读者姓名", "en:Reader Name"},
            info = {"读者姓名", "en:Reader name"},
            list = true,
            readonly = true)
    public String readerName;

    @ModelField(
            name = {"图书编号", "en:Book ID"},
            info = {"图书编号", "en:Book ID"},
            list = true,
            link_to = "book.id",
            required = true)
    public String bookId;

    @ModelField(
            name = {"图书名称", "en:Book Name"},
            info = {"图书名称", "en:Book name"},
            list = true,
            readonly = true)
    public String bookName;

    @ModelField(
            name = {"借阅日期", "en:Borrow Date"},
            info = {"图书借阅日期", "en:Book borrow date"},
            list = true,
            show = true,
            readonly = true,
            input_type = InputType.datetime)
    public String borrowDate;

    @ModelField(
            name = {"应还日期", "en:Due Date"},
            info = {"图书应归还日期", "en:Book due date"},
            list = true,
            show = true,
            readonly = true,
            input_type = InputType.datetime)
    public String dueDate;

    @ModelField(
            name = {"归还日期", "en:Return Date"},
            info = {"图书实际归还日期", "en:Book return date"},
            list = true,
            show = true,
            readonly = true,
            input_type = InputType.datetime)
    public String returnDate;

    @ModelField(
            name = {"状态", "en:Status"},
            info = {"借阅状态", "en:Borrow status"},
            list = true,
            readonly = true,
            input_type = InputType.select,
            options = {"borrowing", "returned", "overdue"})
    public String status = "borrowing";

    @ModelField(
            name = {"备注", "en:Remark"},
            info = {"借阅备注信息", "en:Remark information"},
            add = true,
            update = true)
    public String remark;

    @ModelAction(code = "returnBook", icon = "Check", list = true, show = true,
            name = {"归还图书", "en:Return Book"},
            info = {"归还已借阅的图书", "en:Return borrowed book"})
    public void returnBook(Request request) {
        // 优先从 URL 路径中获取 ID，如果没有则从请求参数中获取
        String id = request.getId();
        if (id == null || id.isEmpty()) {
            id = request.getParameter("id");
        }

        Map<String, String> borrowRecord = db.get(id);
        if (borrowRecord != null && "borrowing".equals(borrowRecord.get("status"))) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            borrowRecord.put("returnDate", sdf.format(new Date()));
            borrowRecord.put("status", "returned");

            String bookId = borrowRecord.get("bookId");
            String readerId = borrowRecord.get("readerId");
            Book.increaseAvailable(bookId);
            Reader.decreaseBorrowed(readerId);

            request.getResponse().success(true).msg("归还成功！");
        } else {
            request.getResponse().success(false).msg("归还失败，该记录不存在或已归还！");
        }
    }


    @Override
    public void add(Map<String, String> data) {
        Request request = getCurrentRequest();
        String newId = "BR" + String.format("%03d", idCounter++);
        data.put("id", newId);

        String readerId = data.get("readerId");
        String bookId = data.get("bookId");

        // 检查读者是否存在且状态正常
        Map<String, String> reader = Reader.db.get(readerId);
        if (reader == null) {
            request.getResponse().success(false).msg("读者不存在！");
            return;
        }

        String readerStatus = reader.get("status");
        if (!"active".equals(readerStatus)) {
            request.getResponse().success(false).msg("读者状态为" + readerStatus + "，不能借阅！");
            return;
        }

        // 检查读者是否超过最大借阅数量
        int borrowed = Integer.parseInt(reader.get("borrowedCount"));
        int maxBorrow = Integer.parseInt(reader.get("maxBorrow"));
        if (borrowed >= maxBorrow) {
            request.getResponse().success(false).msg("该读者已借阅" + borrowed + "本书，超过最大借阅数" + maxBorrow + "！");
            return;
        }

        data.put("readerName", reader.get("name"));

        // 检查图书是否存在且有可借数量
        Map<String, String> book = Book.db.get(bookId);
        if (book == null) {
            request.getResponse().success(false).msg("图书不存在！");
            return;
        }

        int available = Integer.parseInt(book.get("available"));
        if (available <= 0) {
            request.getResponse().success(false).msg("该图书已全部借出，暂无可用！");
            return;
        }

        data.put("bookName", book.get("name"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        data.put("borrowDate", sdf.format(now));

        calendar.add(Calendar.DAY_OF_MONTH, 30);
        data.put("dueDate", sdf.format(calendar.getTime()));
        data.put("status", "borrowing");

        Book.decreaseAvailable(bookId);
        Reader.increaseBorrowed(readerId);

        db.put(newId, new HashMap<>(data));
        request.getResponse().success(true).msg("借阅成功！");
    }

    @Override
    public java.util.List<String[]> list(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> result = new ArrayList<>();
        java.util.List<Map<String, String>> filtered = new ArrayList<>();

        for (Map<String, String> record : db.values()) {
            if (matchesQuery(record, query)) {
                filtered.add(record);
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
        for (Map<String, String> record : db.values()) {
            if (matchesQuery(record, query)) {
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
    public void delete(String id) throws Exception {
        db.remove(id);
    }
}
