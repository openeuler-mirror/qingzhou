package qingzhou.store;

import java.util.Set;

public interface Store {
    void put(String key, String value);

    String get(String key);

    void delete(String key);

    boolean contains(String key);

    Set<String> keys();
}
