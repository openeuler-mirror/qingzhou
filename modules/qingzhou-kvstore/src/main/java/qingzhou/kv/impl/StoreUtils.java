package qingzhou.kv.impl;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import qingzhou.kv.KeyValueStore;

/** 契约校验：namespace/key 正则、value 非空与字节上限，非法一律拒绝（抛 IllegalArgumentException） */
final class StoreUtils {
    private static final Pattern KEY = Pattern.compile(KeyValueStore.VALID_KEY);
    private static final Pattern NAMESPACE = Pattern.compile(KeyValueStore.VALID_NAMESPACE);

    private StoreUtils() {
    }

    static void checkNamespace(String namespace) {
        if (namespace == null || !NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("invalid namespace: " + namespace);
        }
    }

    static void checkKey(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("invalid key: " + key);
        }
    }

    static void checkValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value required");
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > KeyValueStore.MAX_VALUE_BYTES) {
            throw new IllegalArgumentException("value exceeds " + KeyValueStore.MAX_VALUE_BYTES + " bytes");
        }
    }
}
