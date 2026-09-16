package qingzhou.store.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import qingzhou.store.Store;

public class MemoryStore implements Store {
    private final Map<String, String> data = new ConcurrentHashMap<>();

    @Override
    public void put(String key, String value) {
        data.put(key, value);
    }

    @Override
    public String get(String key) {
        return data.get(key);
    }

    @Override
    public void delete(String key) {
        data.remove(key);
    }

    @Override
    public boolean contains(String key) {
        return data.containsKey(key);
    }
}
