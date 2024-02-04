package qingzhou.app.master;

import qingzhou.framework.api.DataStore;

import java.util.List;
import java.util.Map;

public class ReadOnlyDataStore implements DataStore {
    private final DataAdapter dataAdapter;

    public ReadOnlyDataStore(DataAdapter dataAdapter) {
        this.dataAdapter = dataAdapter;
    }

    @Override
    public List<Map<String, String>> getAllData(String type) {
        return dataAdapter.getAllData(type);
    }

    @Override
    public void addData(String type, String id, Map<String, String> properties) {
        throw new RuntimeException("No Support.");
    }

    @Override
    public void updateDataById(String type, String id, Map<String, String> data) {
        throw new RuntimeException("No Support.");
    }

    @Override
    public void deleteDataById(String type, String id) {
        throw new RuntimeException("No Support.");
    }

    public interface DataAdapter {
        List<Map<String, String>> getAllData(String type);
    }
}
