package qingzhou.app.system.setting;

import java.util.Map;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Update;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;
import qingzhou.config.Config;

@Model(code = "web", icon = "link",
        menu = Main.SETTING_MENU, order = 2,
        entrance = Update.ACTION_EDIT,
        name = {"Web", "en:Web"},
        info = {"设置控制台相关参数，如 HTTP、Servlet 等。注：这些参数变更后，在下次启动后生效。",
                "en:Set console-related parameters, such as HTTP and servlets. Note: After these parameters are changed, they will take effect after the next startup."})
public class Web extends ModelBase implements Update {
    @ModelField(
            type = FieldType.number,
            required = true,
            port = true,
            name = {"服务端口", "en:Port"},
            info = {"设置控制台服务使用的端口。",
                    "en:Set the port to be used by the console service."})
    public Integer port;

    @ModelField(
            required = true,
            name = {"访问路径", "en:Context Root"},
            info = {"设置访问控制台服务的根路径。",
                    "en:Set the root path to access console services."})
    public String contextRoot;

    @ModelField(
            type = FieldType.number,
            required = true,
            min = 1,
            name = {"最大 POST 长度", "en:Max Post Size"},
            info = {"设置接收 POST 数据的最大长度，单位：字节。",
                    "en:Sets the maximum length of received POST data in bytes."})
    public Integer maxPostSize;

    @Override
    public Map<String, String> editData(String id) {
        Config config = Main.getService(Config.class);
        qingzhou.config.Web web = config.getConsole().getWeb();
        return ModelUtil.getPropertiesFromObj(web);
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        Config config = Main.getService(Config.class);
        qingzhou.config.Web web = config.getConsole().getWeb();
        ModelUtil.setPropertiesToObj(web, data);
        config.setWeb(web); // 最后没问题再写入配置文件
    }
}
