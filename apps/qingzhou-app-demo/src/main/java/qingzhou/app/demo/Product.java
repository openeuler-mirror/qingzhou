package qingzhou.app.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.api.InputType;
import qingzhou.api.Request;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelAction;
import qingzhou.api.annotations.ModelField;
import qingzhou.api.action.Add;
import qingzhou.api.action.Delete;
import qingzhou.api.action.Monitor;
import qingzhou.api.action.Namespace;
import qingzhou.api.action.Page;
import qingzhou.api.action.Show;
import qingzhou.api.action.Update;
import qingzhou.logger.Logger;

/**
 * 产品管理：演示 CRUD + 监控（Monitor）+ 命名空间（Namespace）+ 条件按钮（display）+ 批量操作。
 */
@Model(code = "product", order = 3,
        name = {"产品", "en:Product"},
        info = {"产品信息管理，演示Monitor和自定义Action", "en:Product management with Monitor"},
        icon = "Box",
        menu = "advanced")
public class Product extends AbstractCrudModel implements Page, Show, Add, Update, Delete, Monitor, Namespace {
    private static final Map<String, Map<String, String>> db = new HashMap<>();

    public Product() {
        if (!db.isEmpty()) return;

        db.put("P001", row("P001", "笔记本电脑", "5999.00", "50", "120", "onsale"));
        db.put("P002", row("P002", "无线鼠标", "129.00", "200", "350", "onsale"));
        db.put("P003", row("P003", "机械键盘", "399.00", "0", "80", "offshelf"));
        idCounter = 4;
    }

    private static Map<String, String> row(String id, String name, String price, String stock,
                                           String sales, String status) {
        Map<String, String> p = new HashMap<>();
        p.put("id", id);
        p.put("name", name);
        p.put("price", price);
        p.put("stock", stock);
        p.put("sales", sales);
        p.put("status", status);
        p.put("createdAt", "2026-01-10 09:00:00");
        return p;
    }

    @Override
    protected Map<String, Map<String, String>> data() {
        return db;
    }

    @Override
    protected String genId(Map<String, String> d) {
        String id = "P" + nextSeq();
        d.put("id", id);
        d.put("sales", "0");
        d.put("createdAt", DateKit.now());
        return id;
    }

    // ============ 列表行内条件按钮：display 决定按钮何时出现 ============

    /** 仅在「未上架」时显示：点击上架。 */
    @ModelAction(code = "onshell", icon = "ArrowUp",
            name = {"上架", "en:On Shelf"},
            info = {"将产品上架销售。非上架状态时显示。", "en:Put product on shelf. Shown when not onsale."},
            display = "status!=onsale", list = true)
    public void onShell(Request request) {
        Map<String, String> p = db.get(request.getId());
        if (p != null) p.put("status", "onsale");
    }

    /** 仅在「已上架」时显示：点击下架。 */
    @ModelAction(code = "offshelf", icon = "ArrowDown",
            name = {"下架", "en:Off Shelf"},
            info = {"将产品下架。上架状态时显示。", "en:Take product off shelf. Shown when onsale."},
            display = "status==onsale", list = true)
    public void offShell(Request request) {
        Map<String, String> p = db.get(request.getId());
        if (p != null) p.put("status", "offshelf");
    }

    /** 组合条件（AND）：上架且有库存时才显示「推广」。 */
    @ModelAction(code = "promote", icon = "Promotion",
            name = {"推广", "en:Promote"},
            info = {"推广产品。上架且有库存时显示。", "en:Promote product. Shown when onsale and in stock."},
            display = "status==onsale & stock>0", list = true)
    public void promote(Request request) {
        request.getResponse().data("推广操作已触发");
    }

    /** 组合条件（OR）：已下架或无库存时才显示「停产」。 */
    @ModelAction(code = "discontinue", icon = "CloseBold",
            name = {"停产", "en:Discontinue"},
            info = {"停产品。已下架或无库存时显示。", "en:Discontinue product. Shown when offshelf or out of stock."},
            display = "status==offshelf | stock==0", list = true)
    public void discontinue(Request request) {
        request.getResponse().data("停产操作已触发");
    }

    /** 工具栏批量操作（始终显示、支持多选）。 */
    @ModelAction(code = "batchDelete", icon = "Delete",
            name = {"批量删除", "en:Batch Delete"},
            info = {"批量删除选中的产品。Toolbar操作，始终显示。", "en:Batch delete selected products. Toolbar action, always shown."},
            list_head = true, batch = true)
    public void batchDelete(Request request) {
        String ids = request.getParameter("ids");
        if (ids != null && !ids.isEmpty()) {
            for (String id : ids.split(",")) db.remove(id.trim());
        }
    }

    /** 监控：返回各产品销量，供前端绘制柱状图。 */
    @Override
    public Map<String, String> monitor(String id) {
        List<String> labels = new ArrayList<>();
        List<String> sales = new ArrayList<>();
        for (Map<String, String> p : db.values()) {
            labels.add(p.get("name"));
            sales.add(p.get("sales"));
        }
        Map<String, String> m = new HashMap<>();
        m.put("labels", String.join(",", labels));
        m.put("sales", String.join(",", sales));
        return m;
    }

    // ============ 命名空间：模拟多租户/多空间切换 ============

    private String currentSpace;

    @Override
    public void namespace(String id) throws Exception {
        currentSpace = id;
        getAppContext().getService(Logger.class).info("切换到：" + id);
    }

    @Override
    public String current() {
        return currentSpace;
    }

    @ModelField(id = true,
            name = {"产品ID", "en:Product ID"},
            info = {"产品唯一标识", "en:Product unique identifier"},
            list = true, show = true, readonly = true)
    public String id;

    @ModelField(name = {"产品名称", "en:Product Name"},
            info = {"产品名称", "en:Product name"},
            list = true, search = true, add = true, update = true, required = true)
    public String name;

    @ModelField(name = {"价格", "en:Price"},
            info = {"产品单价", "en:Product price"},
            list = true, add = true, update = true, input_type = InputType.decimal, min_value = 0, group = "Inventory")
    public String price;

    @ModelField(name = {"库存", "en:Stock"},
            info = {"产品库存数量", "en:Product stock quantity"},
            list = true, add = true, update = true, input_type = InputType.number, min_value = 0, group = "Inventory")
    public Integer stock;

    @ModelField(name = {"销量", "en:Sales"},
            info = {"产品销量", "en:Product sales volume"},
            list = true, add = false, input_type = InputType.number, numeric = true, group = "Inventory")
    public Integer sales;

    @ModelField(name = {"状态", "en:Status"},
            info = {"产品状态", "en:Product status"},
            list = true, add = true, update = true, input_type = InputType.select,
            options = {"onsale", "offshelf", "discontinued"})
    public String status = "onsale";

    @ModelField(name = {"创建时间", "en:Created"},
            info = {"创建时间", "en:Creation time"},
            list = true, show = true, readonly = true, input_type = InputType.datetime)
    public String createdAt;
}
