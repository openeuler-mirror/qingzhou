package qingzhou.app.instance;

import qingzhou.api.DataStore;

import java.util.Map;

public abstract class ReadOnlyDataStore implements DataStore {
    @Override
    public final void addData(String type, String id, Map<String, String> properties) {
        throw new RuntimeException("No Support.");
    }

    @Override
    public final void updateDataById(String type, String id, Map<String, String> data) {
        throw new RuntimeException("No Support.");
    }

    @Override
    public final void deleteDataById(String type, String id) {
        throw new RuntimeException("No Support.");
    }
}
