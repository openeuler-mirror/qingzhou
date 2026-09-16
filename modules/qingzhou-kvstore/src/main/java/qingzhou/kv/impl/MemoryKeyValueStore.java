package qingzhou.kv.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import org.osgi.service.component.annotations.Component;

import qingzhou.kv.KeyValueStore;

/** KV 内存实现：与文件实现同语义，重启即失效，type=memory 服务注册 */
@Component(immediate = true, property = "type=memory", service = KeyValueStore.class)
public class MemoryKeyValueStore implements KeyValueStore {
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> store = new ConcurrentHashMap<>();
    /** per-key 互斥锁（update 原子读改写），键域受契约约束有界 */
    private final ConcurrentHashMap<String, Object> keyLocks = new ConcurrentHashMap<>();

    @Override
    public String get(String namespace, String key) {
        StoreUtils.checkNamespace(namespace);
        StoreUtils.checkKey(key);
        ConcurrentHashMap<String, String> ns = store.get(namespace);
        return ns == null ? null : ns.get(key);
    }

    @Override
    public void put(String namespace, String key, String value) {
        StoreUtils.checkNamespace(namespace);
        StoreUtils.checkKey(key);
        StoreUtils.checkValue(value);
        store.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    @Override
    public String update(String namespace, String key, UnaryOperator<String> fn) {
        StoreUtils.checkNamespace(namespace);
        StoreUtils.checkKey(key);
        if (fn == null) throw new IllegalArgumentException("fn required");
        Object lock = keyLocks.computeIfAbsent(namespace + "/" + key, k -> new Object());
        synchronized (lock) {
            ConcurrentHashMap<String, String> ns = store.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());
            String next = fn.apply(ns.get(key));
            if (next == null) {
                ns.remove(key);
            } else {
                ns.put(key, next);
            }
            return next;
        }
    }

    @Override
    public boolean delete(String namespace, String key) {
        StoreUtils.checkNamespace(namespace);
        StoreUtils.checkKey(key);
        ConcurrentHashMap<String, String> ns = store.get(namespace);
        return ns != null && ns.remove(key) != null;
    }

    @Override
    public boolean exists(String namespace, String key) {
        StoreUtils.checkNamespace(namespace);
        StoreUtils.checkKey(key);
        ConcurrentHashMap<String, String> ns = store.get(namespace);
        return ns != null && ns.containsKey(key);
    }
}
