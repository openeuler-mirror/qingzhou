package qingzhou.app.demo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.InputType;
import qingzhou.api.Request;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelAction;
import qingzhou.api.annotations.ModelField;
import qingzhou.api.action.Add;
import qingzhou.api.action.Delete;
import qingzhou.api.action.Download;
import qingzhou.api.action.Page;
import qingzhou.api.action.Show;
import qingzhou.api.action.Update;

/**
 * 订单管理：演示复杂表单（单选/多选/级联/日期等），主键为业务单号 {@code orderNo}。
 */
@Model(code = "order", order = 2,
        name = {"订单", "en:Order"},
        info = {"订单信息管理，演示复杂表单", "en:Order management with complex forms"},
        icon = "Document",
        menu = "basic")
public class Order extends AbstractCrudModel implements Page, Show, Add, Update, Delete, Download {
    private static final Map<String, Map<String, String>> db = new HashMap<>();

    public Order() {
        if (!db.isEmpty()) return;

        String[] customers = {"王五", "赵六", "孙七", "周八", "吴九", "郑十", "冯十一", "陈十二", "褚十三", "卫十四"};
        String[] payMethods = {"wechat", "alipay", "bankcard", "cash", "wechat"};
        String[] deliveryMethods = {"express", "pickup", "home_delivery", "self_pickup", "express"};
        String[] statuses = {"pending", "paid", "shipped", "delivered", "cancelled"};

        for (int i = 1; i <= 25; i++) {
            Map<String, String> o = new HashMap<>();
            o.put("orderNo", "ORD2026" + String.format("%04d", i));
            o.put("customerName", customers[i % customers.length]);
            o.put("phone", "138" + String.format("%08d", i));
            o.put("address", "某某路" + i + "号");
            o.put("amount", String.format("%.2f", 100.0 + (i * 73.5) % 5000));
            o.put("payMethod", payMethods[i % payMethods.length]);
            o.put("deliveryMethod", deliveryMethods[i % deliveryMethods.length]);
            o.put("remark", i % 3 == 0 ? "加急" : "");
            o.put("orderTime", "2026-04-" + String.format("%02d", (i % 28) + 1) + " " + String.format("%02d", (i * 3) % 24) + ":00:00");
            o.put("status", statuses[i % statuses.length]);
            db.put(o.get("orderNo"), o);
        }
        idCounter = 26;
    }

    @Override
    protected Map<String, Map<String, String>> data() {
        return db;
    }

    @Override
    protected String genId(Map<String, String> d) {
        // 业务主键：日期 + 自增序号，如 ORD20261113001
        String id = "ORD" + DateKit.dateNum() + nextSeq();
        d.put("orderNo", id);
        d.put("orderTime", DateKit.now());
        return id;
    }

    /** 自定义 Action 示例：仅回显一段文本。 */
    @ModelAction
    public void abc(Request request) {
        request.getResponse().data("我来自 ModelAction.。");
    }

    @ModelField(id = true,
            name = {"订单号", "en:Order No"},
            info = {"订单唯一标识", "en:Order unique identifier"},
            list = true, show = true, readonly = true)
    public String orderNo;

    @ModelField(name = {"客户姓名", "en:Customer"},
            info = {"客户姓名", "en:Customer name"},
            list = true, search = true, add = true, update = true, required = true)
    public String customerName;

    @ModelField(name = {"联系电话", "en:Phone"},
            info = {"客户联系电话", "en:Customer phone number"},
            list = true, add = true, update = true, pattern = "^1[3-9]\\d{9}$", group = "Delivery")
    public String phone;

    @ModelField(name = {"配送地址", "en:Address"},
            info = {"详细配送地址", "en:Delivery address"},
            input_type = InputType.textarea, add = true, update = true, group = "Delivery")
    public String address;

    @ModelField(name = {"订单金额", "en:Amount"},
            info = {"订单总金额", "en:Order total amount"},
            list = true, add = true, update = true, input_type = InputType.decimal, min_value = 0, group = "Order")
    public String amount;

    @ModelField(name = {"支付方式", "en:Pay Method"},
            info = {"支付方式", "en:Payment method"},
            list = true, add = true, update = true, input_type = InputType.radio,
            options = {"wechat", "alipay", "bankcard", "cash"}, group = "Order")
    public String payMethod;

    @ModelField(name = {"配送方式", "en:Delivery"},
            info = {"配送方式", "en:Delivery method"},
            add = true, update = true, input_type = InputType.checkbox,
            options = {"express", "pickup", "home_delivery", "self_pickup"}, group = "Delivery")
    public String deliveryMethod;

    @ModelField(name = {"备注", "en:Remark"},
            info = {"订单备注", "en:Order remark"},
            input_type = InputType.kv, add = true, update = true, group = "Delivery")
    public String remark;

    @ModelField(name = {"下单时间", "en:Order Time"},
            info = {"下单时间", "en:Order time"},
            list = true, show = true, readonly = true, input_type = InputType.datetime)
    public String orderTime;

    @ModelField(name = {"订单状态", "en:Status"},
            info = {"当前订单状态", "en:Order status"},
            list = true, add = true, update = true, input_type = InputType.select,
            options = {"pending", "paid", "shipped", "delivered", "cancelled"})
    public String status;

    @Override
    public File file(String id) {
        return new File(getAppContext().getRoot(), "apps");
    }
}
