package qingzhou.app.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.*;
import qingzhou.api.type.*;

@Model(code = "product", order = 3,
        name = {"产品", "en:Product"},
        info = {"产品信息管理，演示Monitor和自定义Action", "en:Product management with Monitor"},
        icon = "Box",
        menu = "advanced")
public class Product extends qingzhou.api.ModelBase implements List, Show, Add, Update, Delete, Monitor {
    public static final Map<String, Map<String, String>> db = new HashMap<>();
    private static int idCounter = 1;

    public Product() {
        if (!db.isEmpty()) return;

        Map<String, String> p1 = new HashMap<>();
        p1.put("id", "P001");
        p1.put("name", "笔记本电脑");
        p1.put("price", "5999.00");
        p1.put("stock", "50");
        p1.put("sales", "120");
        p1.put("status", "onsale");
        p1.put("createdAt", "2026-01-10 09:00:00");
        db.put(p1.get("id"), p1);

        Map<String, String> p2 = new HashMap<>();
        p2.put("id", "P002");
        p2.put("name", "无线鼠标");
        p2.put("price", "129.00");
        p2.put("stock", "200");
        p2.put("sales", "350");
        p2.put("status", "onsale");
        p2.put("createdAt", "2026-02-15 14:30:00");
        db.put(p2.get("id"), p2);

        Map<String, String> p3 = new HashMap<>();
        p3.put("id", "P003");
        p3.put("name", "机械键盘");
        p3.put("price", "399.00");
        p3.put("stock", "0");
        p3.put("sales", "80");
        p3.put("status", "offshelf");
        p3.put("createdAt", "2026-03-01 11:00:00");
        db.put(p3.get("id"), p3);
        idCounter = 4;
    }

    @ModelField(id = true,
            name = {"产品ID", "en:Product ID"},
            info = {"产品唯一标识", "en:Product unique identifier"},
            list = true,
            show = true,
            readonly = true)
    public String id;

    @ModelField(
            name = {"产品名称", "en:Product Name"},
            info = {"产品名称", "en:Product name"},
            list = true,
            search = true,
            add = true,
            update = true,
            required = true)
    public String name;

    @ModelField(
            name = {"价格", "en:Price"},
            info = {"产品单价", "en:Product price"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.decimal,
            min = 0,
            group = {"库存与价格", "en:Inventory & Price"})
    public String price;

    @ModelField(
            name = {"库存", "en:Stock"},
            info = {"产品库存数量", "en:Product stock quantity"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.number,
            min = 0,
            group = {"库存与价格", "en:Inventory & Price"})
    public Integer stock;

    @ModelField(
            name = {"销量", "en:Sales"},
            info = {"产品销量", "en:Product sales volume"},
            list = true,
            add = false,
            input_type = InputType.number,
            numeric = true,
            group = {"库存与价格", "en:Inventory & Price"})
    public Integer sales;

    @ModelField(
            name = {"状态", "en:Status"},
            info = {"产品状态", "en:Product status"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.select,
            options = {"onsale", "offshelf", "discontinued"})
    public String status = "onsale";

    @ModelField(
            name = {"创建时间", "en:Created"},
            info = {"创建时间", "en:Creation time"},
            list = true,
            show = true,
            readonly = true,
            input_type = InputType.datetime)
    public String createdAt;

    @ModelAction(code = "onshell", icon = "ArrowUp",
            name = {"上架", "en:On Shelf"},
            info = {"将产品上架销售。非上架状态时显示。", "en:Put product on shelf. Shown when not onsale."},
            display = "status!=onsale",
            list = true)
    public void onShell(Request request) {
        String id = request.getId();
        Map<String, String> product = db.get(id);
        if (product != null) {
            product.put("status", "onsale");
        }
    }

    @ModelAction(code = "offshelf", icon = "ArrowDown",
            name = {"下架", "en:Off Shelf"},
            info = {"将产品下架。上架状态时显示。", "en:Take product off shelf. Shown when onsale."},
            display = "status==onsale",
            list = true)
    public void offShell(Request request) {
        String id = request.getId();
        Map<String, String> product = db.get(id);
        if (product != null) {
            product.put("status", "offshelf");
        }
    }

    // 演示组合条件（AND）：上架且有库存时才显示"推广"按钮
    @ModelAction(code = "promote", icon = "Promotion",
            name = {"推广", "en:Promote"},
            info = {"推广产品。上架且有库存时显示。", "en:Promote product. Shown when onsale and in stock."},
            display = "status==onsale & stock>0",
            list = true)
    public void promote(Request request) {
        // 推广操作示例：不做实际数据变更，仅返回提示
        request.getResponse().data("推广操作已触发");
    }

    // 演示组合条件（OR）：已下架或无库存时才显示"停产"按钮
    @ModelAction(code = "discontinue", icon = "CloseBold",
            name = {"停产", "en:Discontinue"},
            info = {"停产品。已下架或无库存时显示。", "en:Discontinue product. Shown when offshelf or out of stock."},
            display = "status==offshelf | stock==0",
            list = true)
    public void discontinue(Request request) {
        // 停产操作示例
        request.getResponse().data("停产操作已触发");
    }

    // 演示 toolbar 操作（无 display 条件，始终显示）
    @ModelAction(code = "batchDelete", icon = "Delete",
            name = {"批量删除", "en:Batch Delete"},
            info = {"批量删除选中的产品。Toolbar操作，始终显示。", "en:Batch delete selected products. Toolbar action, always shown."},
            list_head = true,
            batch = true)
    public void batchDelete(Request request) {
        String ids = request.getParameter("ids");
        if (ids != null && !ids.isEmpty()) {
            for (String id : ids.split(",")) {
                db.remove(id.trim());
            }
        }
    }

    @Override
    public java.util.List<String[]> list(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> result = new ArrayList<>();
        java.util.List<Map<String, String>> filtered = new ArrayList<>();

        for (Map<String, String> product : db.values()) {
            if (matchesQuery(product, query)) {
                filtered.add(product);
            }
        }

        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, filtered.size());

        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> p = filtered.get(i);
            String[] data = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                String value = p.get(listFields[j]);
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
        for (Map<String, String> product : db.values()) {
            if (matchesQuery(product, query)) {
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
    public Map<String, String> monitor(String id) {
        Map<String, String> monitorData = new HashMap<>();
        java.util.List<Integer> salesData = new ArrayList<>();
        java.util.List<String> labels = new ArrayList<>();

        for (Map<String, String> product : db.values()) {
            labels.add(product.get("name"));
            salesData.add(Integer.parseInt(product.get("sales")));
        }

        monitorData.put("labels", String.join(",", labels));
        monitorData.put("sales", String.join(",", salesData.stream().map(String::valueOf).toArray(String[]::new)));
        return monitorData;
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        String newId = "P" + String.format("%03d", idCounter++);
        data.put("id", newId);
        data.put("sales", "0");
        data.put("createdAt", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        db.put(newId, new HashMap<>(data));
    }

    @Override
    public void update(String id, Map<String, String> data) throws Exception {
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
