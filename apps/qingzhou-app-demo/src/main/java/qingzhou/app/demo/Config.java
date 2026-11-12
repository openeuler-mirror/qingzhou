package qingzhou.app.demo;

import java.util.HashMap;
import java.util.Map;

import qingzhou.api.InputType;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelAction;
import qingzhou.api.annotations.ModelField;
import qingzhou.api.annotations.OverrideDefaultActions;
import qingzhou.api.action.Add;
import qingzhou.api.action.Delete;
import qingzhou.api.action.Namespace;
import qingzhou.api.action.Page;
import qingzhou.api.action.Show;
import qingzhou.api.action.Update;
import qingzhou.logger.Logger;

/**
 * 系统配置管理：演示「以业务字段作主键」——此处主键即 {@code configKey}（而非自增 id）。
 * 另演示 {@link OverrideDefaultActions} 覆盖默认 Action 的元数据，以及命名空间切换。
 */
@Model(code = "config", order = 4,
        name = {"配置", "en:Config"},
        info = {"系统配置管理", "en:System configuration management"},
        icon = "Setting",
        menu = "system")
@OverrideDefaultActions({
        @ModelAction(code = "delete", name = {"删除配置", "en:Delete Config"}, order = 0),
        @ModelAction(code = "add", name = {"增加配置", "en:Add Config"}, icon = "Plus", order = 10),
})
@ModelAction(code = "show", name = {"显示配置", "en:Show Config"}, icon = "Plus", order = 100)
public class Config extends AbstractCrudModel implements Page, Show, Add, Update, Delete, Namespace {
    private static final Map<String, Map<String, String>> db = new HashMap<>();

    public Config() {
        if (!db.isEmpty()) return;

        put("system.name", "轻舟管理平台", "系统名称", "2026-01-01 00:00:00");
        put("system.version", "1.0.0", "系统版本号", "2026-01-01 00:00:00");
        put("email.smtp.host", "smtp.example.com", "SMTP服务器地址", "2026-01-01 00:00:00");
        put("email.smtp.port", "587", "SMTP服务器端口", "2026-01-01 00:00:00");
    }

    private static void put(String key, String value, String desc, String created) {
        Map<String, String> c = new HashMap<>();
        c.put("configKey", key);
        c.put("configValue", value);
        c.put("description", desc);
        c.put("createdAt", created);
        db.put(key, c);
    }

    @Override
    protected Map<String, Map<String, String>> data() {
        return db;
    }

    @Override
    protected String genId(Map<String, String> d) {
        // 主键即 configKey，新增时由表单提供，无需自增
        d.put("createdAt", DateKit.now());
        return d.get("configKey");
    }

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
            name = {"配置键", "en:Config Key"},
            info = {"配置唯一标识键", "en:Configuration unique key"},
            list = true)
    public String configKey;

    @ModelField(name = {"配置值", "en:Config Value"},
            info = {"配置值内容", "en:Configuration value"},
            list = true, search = true, add = true, update = true,
            input_type = InputType.textarea, group = "ConfigDetails")
    public String configValue;

    @ModelField(name = {"描述", "en:Description"},
            info = {"配置项描述信息", "en:Configuration description"},
            list = true, add = true, update = true, group = "ConfigDetails")
    public String description;

    @ModelField(name = {"创建时间", "en:Created"},
            info = {"创建时间", "en:Creation time"},
            list = true, show = true, readonly = true, input_type = InputType.datetime)
    public String createdAt;
}
