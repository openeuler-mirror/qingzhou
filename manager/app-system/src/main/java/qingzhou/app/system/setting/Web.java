package qingzhou.app.system.setting;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Updatable;
import qingzhou.app.system.Main;
import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Model(code = DeployerConstants.MODEL_WEB,
        icon = "cog",
        menu = Main.SETTING_MENU,
        order = 5,
        entrance = Updatable.ACTION_EDIT,
        name = {"控制台参数", "en:Console parameters"},
        info = {"管理控制台相关参数", "en:Management console related parameters"})
public class Web extends ModelBase implements Updatable {
    @ModelField(type = FieldType.number, port = true, required = true, name = {"端口", "en:port"}, info = {"控制台端口", "en:console port. "})
    public Integer port = 9000;

    @ModelField(required = true, name = {"访问上下文", "en:contextRoot"}, info = {"控制台的访问上下文(contextRoot)", "en:Console access context."})
    public String contextRoot = "/console";


    @ModelField(type = FieldType.kv, name = {"Servlet属性", "en:servlet Properties"}, info = {"Servlet属性", "en:servlet Properties."})
    public String servletProperties;

    @Override
    public Map<String, String> showData(String id) {
        Map<String, String> map = new HashMap<>();
        Config config = Main.getService(Config.class);
        Console console = config.getConsole();
        map.put("servletProperties", propertiesToString(console.getServletProperties()));
        map.put("contextRoot", console.getContextRoot());
        map.put("port", console.getPort() + "");
        return map;
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        Config config = Main.getService(Config.class);
        if (!Utils.isBlank(data.get("servletProperties"))){
            config.setServletProperties(stringToProperties(data.get("servletProperties")));
        }
        config.setContextRoot(data.get("contextRoot"));
        config.setConsolePort(data.get("port"));
    }

    private String propertiesToString(Properties properties) {
        StringBuilder servletPropertiesValue = new StringBuilder();
        for (Object key : properties.keySet()) {
            servletPropertiesValue.append(key.toString()).append("=").append(properties.getProperty(key.toString())).append(",");
        }
        String result = servletPropertiesValue.toString();
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private Properties stringToProperties(String data) {
        Properties properties = new Properties();
        final String[] split = data.split(",");
        for (String kv : split) {
            String[] split1 = kv.split("=");
            properties.put(split1[0], split1[1]);
        }

        return properties;
    }
}
