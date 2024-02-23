package qingzhou.app.master;

import qingzhou.api.DataStore;
import qingzhou.api.ListModel;
import qingzhou.config.ConfigManager;
import qingzhou.framework.util.StringUtil;

import java.util.List;
import java.util.Map;

public class ConsoleDataStore implements DataStore {
    @Override
    public List<Map<String, String>> getAllData(String type) {
        return Main.getFc().getService(ConfigManager.class).getConfigList("//" + type);
    }

    @Override
    public void addData(String type, String id, Map<String, String> properties) {
        String tags = "//" + type + "s";
        if (StringUtil.notBlank(id)) {
            properties.put(ListModel.FIELD_NAME_ID, id);
        }
        ConfigManager configManager = Main.getFc().getService(ConfigManager.class);
        if (!configManager.existsConfig(tags)) {
            configManager.addConfig("/root/console", type + "s", null);
        }
        configManager.addConfig(tags, type, properties);
    }

    @Override
    public void updateDataById(String type, String id, Map<String, String> data) {
        deleteDataById(type, id);
        addData(type, id, data);
    }

    @Override
    public void deleteDataById(String type, String id) {
        Main.getFc().getService(ConfigManager.class).deleteConfig("//" + type + "s/" + type + "[@id='" + id + "']");
    }
}