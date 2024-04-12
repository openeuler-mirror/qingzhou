package qingzhou.deployer.impl;

import qingzhou.api.DataStore;
import qingzhou.api.type.Listable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MemoryDataStore implements DataStore {
    private final Map<String, List<Map<String, String>>> dataStore = new ConcurrentHashMap<>();

    @Override
    public List<Map<String, String>> getAllData(String type) {
        return dataStore.computeIfAbsent(type, s -> new CopyOnWriteArrayList<>());
    }

    @Override
    public void addData(String type, String id, Map<String, String> properties) {
        dataStore.get(type).add(properties);
    }

    @Override
    public void updateDataById(String type, String id, Map<String, String> data) {
        for (Map<String, String> map : dataStore.get(type)) {
            if (map.get(Listable.FIELD_NAME_ID).equals(id)) {
                map.putAll(data);
            }
        }
    }

    @Override
    public void deleteDataById(String type, String id) {
        List<Map<String, String>> maps = dataStore.get(type);
        for (int i = 0; i < maps.size(); i++) {
            if (maps.get(i).get(Listable.FIELD_NAME_ID).equals(id)) {
                maps.remove(i);
                return;
            }
        }
    }
}
