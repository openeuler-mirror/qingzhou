package qingzhou.app.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.*;
import qingzhou.api.type.*;

@Model(code = "order", order = 2,
        name = {"订单", "en:Order"},
        info = {"订单信息管理，演示复杂表单", "en:Order management with complex forms"},
        icon = "Document",
        menu = "basic")
public class Order extends qingzhou.api.ModelBase implements List, Show, Add, Update, Delete {
    public static final Map<String, Map<String, String>> db = new HashMap<>();
    private int idCounter = 1;

    public Order() {
        Map<String, String> o1 = new HashMap<>();
        o1.put("orderNo", "ORD20260401001");
        o1.put("customerName", "王五");
        o1.put("phone", "13800138001");
        o1.put("address", "北京市朝阳区某某路123号");
        o1.put("amount", "299.50");
        o1.put("payMethod", "wechat");
        o1.put("deliveryMethod", "express");
        o1.put("remark", "客户要求:尽快发货");
        o1.put("orderTime", "2026-04-01 10:30:00");
        o1.put("status", "pending");
        db.put(o1.get("orderNo"), o1);

        Map<String, String> o2 = new HashMap<>();
        o2.put("orderNo", "ORD20260402002");
        o2.put("customerName", "赵六");
        o2.put("phone", "13900139002");
        o2.put("address", "上海市浦东新区某某街456号");
        o2.put("amount", "1599.00");
        o2.put("payMethod", "alipay");
        o2.put("deliveryMethod", "pickup");
        o2.put("remark", "");
        o2.put("orderTime", "2026-04-02 15:45:00");
        o2.put("status", "shipped");
        db.put(o2.get("orderNo"), o2);
        idCounter = 3;
    }

    @ModelAction
    public void abc(Request request) {
        request.getResponse().data("我来自 ModelAction.。");
    }

    @ModelField(id = true,
            name = {"订单号", "en:Order No"},
            info = {"订单唯一标识", "en:Order unique identifier"},
            list = true,
            show = true,
            readonly = true)
    public String orderNo;

    @ModelField(
            name = {"客户姓名", "en:Customer"},
            info = {"客户姓名", "en:Customer name"},
            list = true,
            search = true,
            add = true,
            update = true,
            required = true)
    public String customerName;

    @ModelField(
            name = {"联系电话", "en:Phone"},
            info = {"客户联系电话", "en:Customer phone number"},
            list = true,
            add = true,
            update = true,
            pattern = "^1[3-9]\\d{9}$",
            group = {"配送信息", "en:Delivery"})
    public String phone;

    @ModelField(
            name = {"配送地址", "en:Address"},
            info = {"详细配送地址", "en:Delivery address"},
            input_type = InputType.textarea,
            add = true,
            update = true,
            group = {"配送信息", "en:Delivery"})
    public String address;

    @ModelField(
            name = {"订单金额", "en:Amount"},
            info = {"订单总金额", "en:Order total amount"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.decimal,
            min = 0,
            group = {"订单详情", "en:Order Details"})
    public String amount;

    @ModelField(
            name = {"支付方式", "en:Pay Method"},
            info = {"支付方式", "en:Payment method"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.radio,
            options = {"wechat", "alipay", "bankcard", "cash"},
            group = {"订单详情", "en:Order Details"})
    public String payMethod;

    @ModelField(
            name = {"配送方式", "en:Delivery"},
            info = {"配送方式", "en:Delivery method"},
            add = true,
            update = true,
            input_type = InputType.checkbox,
            options = {"express", "pickup", "home_delivery", "self_pickup"},
            group = {"配送信息", "en:Delivery"})
    public String deliveryMethod;

    @ModelField(
            name = {"备注", "en:Remark"},
            info = {"订单备注", "en:Order remark"},
            input_type = InputType.kv,
            add = true,
            update = true,
            group = {"配送信息", "en:Delivery"})
    public String remark;

    @ModelField(
            name = {"下单时间", "en:Order Time"},
            info = {"下单时间", "en:Order time"},
            list = true,
            show = true,
            readonly = true,
            input_type = InputType.datetime)
    public String orderTime;

    @ModelField(
            name = {"订单状态", "en:Status"},
            info = {"当前订单状态", "en:Order status"},
            list = true,
            add = true,
            update = true,
            input_type = InputType.select,
            options = {"pending", "paid", "shipped", "delivered", "cancelled"})
    public String status;

    @Override
    public java.util.List<String[]> list(Request request, int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> result = new ArrayList<>();
        java.util.List<Map<String, String>> filtered = new ArrayList<>();

        for (Map<String, String> order : db.values()) {
            if (matchesQuery(order, query)) {
                filtered.add(order);
            }
        }

        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, filtered.size());

        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> o = filtered.get(i);
            String[] data = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                String value = o.get(listFields[j]);
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
        for (Map<String, String> order : db.values()) {
            if (matchesQuery(order, query)) {
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
        String newId = "ORD" + new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date()) + String.format("%03d", idCounter++);
        data.put("orderNo", newId);
        data.put("orderTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        db.put(newId, new HashMap<>(data));
    }

    @Override
    public void update(Request request, Map<String, String> data) throws Exception {
        String id = request.getId();
        if (db.containsKey(id)) {
            Map<String, String> existing = db.get(id);
            existing.putAll(data);
            existing.put("orderNo", id);
        }
    }

    @Override
    public void delete(String id) throws Exception {
        db.remove(id);
    }
}
