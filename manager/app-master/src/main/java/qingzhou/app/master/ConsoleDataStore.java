package qingzhou.app.master;

import qingzhou.api.DataStore;

import java.util.List;
import java.util.Map;

public class ConsoleDataStore implements DataStore {
    @Override
    public List<Map<String, String>> getAllData(String type) throws Exception {
        return MasterApp.getService(Config.class).getAllData(type);
    }

    @Override
    public void addData(String type, String id, Map<String, String> properties) throws Exception {
        Config config = MasterApp.getService(Config.class);
        config.addData(type, id, properties);
    }

    @Override
    public void updateDataById(String type, String id, Map<String, String> data) throws Exception {
        deleteDataById(type, id);
        addData(type, id, data);
    }

    @Override
    public void deleteDataById(String type, String id) throws Exception {
        MasterApp.getService(Config.class).deleteDataById(type, id);
    }
}