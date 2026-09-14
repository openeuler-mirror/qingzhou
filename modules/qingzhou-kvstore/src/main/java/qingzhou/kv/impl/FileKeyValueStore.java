package qingzhou.kv.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import org.osgi.service.component.annotations.Component;

import qingzhou.kv.KeyValueStore;

/**
 * KV 文件实现：&lt;instance&gt;/data/kv/&lt;namespace&gt;/&lt;key&gt;.json，重启后数据仍在。
 * update 经 per-key 锁互斥（不同键并行）；存储目录创建失败抛异常使组件激活失败、服务不注册，
 * 消费方据此降级。零配置：目录与上限均由契约决定。
 */
@Component(immediate = true, property = "type=file", service = KeyValueStore.class)
public class FileKeyValueStore implements KeyValueStore {
    private final File root;
    /** per-key 互斥锁（update 原子读改写），键域受契约约束有界 */
    private final ConcurrentHashMap<String, Object> keyLocks = new ConcurrentHashMap<>();

    public FileKeyValueStore() {
        root = new File(System.getProperty("qingzhou.instance"), "data" + File.separator + "kv");
        if (!root.exists() && !root.mkdirs()) {
            throw new IllegalStateException("failed to create kv dir: " + root.getAbsolutePath());
        }
    }

    @Override
    public String get(String namespace, String key) {
        StoreUtils.checkNamespace(namespace);
        StoreUtils.checkKey(key);
        File file = file(namespace, key);
        if (!file.exists()) return null;
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("failed to read kv value: " + e.getMessage(), e);
        }
    }

    @Override
    public void put(String namespace, String key, String value) {
        StoreUtils.checkNamespace(namespace);
        StoreUtils.checkKey(key);
        StoreUtils.checkValue(value);
        try {
            File file = file(namespace, key);
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("failed to create dir: " + parent.getAbsolutePath());
            }
            Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("failed to write kv value: " + e.getMessage(), e);
        }
    }

    @Override
    public String update(String namespace, String key, UnaryOperator<String> fn) {
        StoreUtils.checkNamespace(namespace);
        StoreUtils.checkKey(key);
        if (fn == null) throw new IllegalArgumentException("fn required");
        Object lock = keyLocks.computeIfAbsent(namespace + "/" + key, k -> new Object());
        synchronized (lock) {
            String next = fn.apply(get(namespace, key));
            if (next == null) {
                delete(namespace, key);
            } else {
                put(namespace, key, next);
            }
            return next;
        }
    }

    @Override
    public boolean delete(String namespace, String key) {
        StoreUtils.checkNamespace(namespace);
        StoreUtils.checkKey(key);
        File file = file(namespace, key);
        return file.exists() && file.delete();
    }

    @Override
    public boolean exists(String namespace, String key) {
        StoreUtils.checkNamespace(namespace);
        StoreUtils.checkKey(key);
        return file(namespace, key).exists();
    }

    private File file(String namespace, String key) {
        return new File(new File(root, namespace), key + ".json");
    }
}
