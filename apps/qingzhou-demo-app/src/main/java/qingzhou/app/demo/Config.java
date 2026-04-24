package qingzhou.app.demo;

import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Add;
import qingzhou.api.type.Delete;
import qingzhou.api.type.List;
import qingzhou.api.type.Show;
import qingzhou.api.type.Update;
import qingzhou.api.InputType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Model(code = "config", order = 4,
        name = {"配置", "en:Config"},
        info = {"系统配置管理", "en:System configuration management"},
        icon = "Setting",
        menu = "system")
public class Config extends qingzhou.api.ModelBase implements List, Show, Add, Update, Delete {
    private final Map<String, Map<String, String>> db = new HashMap<>();

    public Config() {
        Map<String, String> c1 = new HashMap<>();
        c1.put("configKey", "system.name");
        c1.put("configValue", "轻舟管理平台");
        c1.put("description", "系统名称");
        c1.put("createdAt", "2026-01-01 00:00:00");
        db.put(c1.get("configKey"), c1);

        Map<String, String> c2 = new HashMap<>();
        c2.put("configKey", "system.version");
        c2.put("configValue", "1.0.0");
        c2.put("description", "系统版本号");
        c2.put("createdAt", "2026-01-01 00:00:00");
        db.put(c2.get("configKey"), c2);

        Map<String, String> c3 = new HashMap<>();
        c3.put("configKey", "email.smtp.host");
        c3.put("configValue", "smtp.example.com");
        c3.put("description", "SMTP服务器地址");
        c3.put("createdAt", "2026-01-01 00:00:00");
        db.put(c3.get("configKey"), c3);

        Map<String, String> c4 = new HashMap<>();
        c4.put("configKey", "email.smtp.port");
        c4.put("configValue", "587");
        c4.put("description", "SMTP服务器端口");
        c4.put("createdAt", "2026-01-01 00:00:00");
        db.put(c4.get("configKey"), c4);
    }

    @ModelField(id = true,
            name = {"配置键", "en:Config Key"},
            info = {"配置唯一标识键", "en:Configuration unique key"},
            list = true,
            show = true,
            readonly = true)
    public String configKey;

    @ModelField(
            name = {"配置值", "en:Config Value"},
            info = {"配置值内容", "en:Configuration value"},
            list = true,
            search = true,
            add = true,
            update = true,
            input_type = InputType.textarea,
            group = {"配置详情", "en:Config Details"})
    public String configValue;

    @ModelField(
            name = {"描述", "en:Description"},
            info = {"配置项描述信息", "en:Configuration description"},
            list = true,
            add = true,
            update = true,
            group = {"配置详情", "en:Config Details"})
    public String description;

    @ModelField(
            name = {"创建时间", "en:Created"},
            info = {"创建时间", "en:Creation time"},
            list = true,
            show = true,
            readonly = true,
            input_type = InputType.datetime)
    public String createdAt;

    @Override
    public java.util.List<String[]> list(Request request, int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> result = new ArrayList<>();
        java.util.List<Map<String, String>> filtered = new ArrayList<>();

        for (Map<String, String> config : db.values()) {
            if (matchesQuery(config, query)) {
                filtered.add(config);
            }
        }

        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, filtered.size());

        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> c = filtered.get(i);
            String[] data = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                String value = c.get(listFields[j]);
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
        for (Map<String, String> config : db.values()) {
            if (matchesQuery(config, query)) {
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
        String key = data.get("configKey");
        data.put("createdAt", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        db.put(key, new HashMap<>(data));
    }

    @Override
    public void update(Request request, Map<String, String> data) throws Exception {
        String key = request.getId();
        if (db.containsKey(key)) {
            Map<String, String> existing = db.get(key);
            existing.putAll(data);
            existing.put("configKey", key);
        }
    }

    @Override
    public void delete(String id) throws Exception {
        db.remove(id);
    }
}
