package qingzhou.app.master;

import qingzhou.api.DataStore;
import qingzhou.api.type.Listable;
import qingzhou.framework.config.Config;
import qingzhou.framework.util.StringUtil;

import java.util.List;
import java.util.Map;

public class ConsoleDataStore implements DataStore {
    @Override
    public List<Map<String, String>> getAllData(String type) throws Exception {
        return MasterApp.getService(Config.class).getAllData("//" + type);
    }

    @Override
    public void addData(String type, String id, Map<String, String> properties) {
        String tags = "//" + type + "s";
        if (StringUtil.notBlank(id)) {
            properties.put(Listable.FIELD_NAME_ID, id);
        }
        Config config = MasterApp.getService(Config.class);
        config.addConfig(tags, type, properties);
    }

    @Override
    public void updateDataById(String type, String id, Map<String, String> data) {
        deleteDataById(type, id);
        addData(type, id, data);
    }

    @Override
    public void deleteDataById(String type, String id) {
        MasterApp.getService(Config.class).deleteConfig("//" + type + "s/" + type + "[@id='" + id + "']");
    }
}