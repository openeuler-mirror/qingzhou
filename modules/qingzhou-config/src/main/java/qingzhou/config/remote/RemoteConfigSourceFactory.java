package qingzhou.config.remote;

import java.util.Map;

import qingzhou.config.remote.etcd.EtcdConfigSource;
import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;

/**
 * 按 qingzhou-config.remote.* 自举参数创建远程配置源；是否启用由调用方先判断。
 */
public final class RemoteConfigSourceFactory {
    public static final String KEY_PREFIX = "qingzhou-config.remote.";
    private static final String DEFAULT_TYPE = "etcd";

    private RemoteConfigSourceFactory() {
    }

    public static RemoteConfigSource create(Map<String, String> bootstrapArgs, HttpClient httpClient, Json json) {
        if (httpClient == null || json == null) {
            throw new IllegalStateException("HttpClient/Json service is required when remote config center is enabled");
        }
        String type = get(bootstrapArgs, "type", DEFAULT_TYPE);
        if (!DEFAULT_TYPE.equalsIgnoreCase(type)) {
            throw new IllegalArgumentException(KEY_PREFIX + "type is not supported: " + type);
        }
        String endpoints = get(bootstrapArgs, "endpoints", null);
        if (endpoints == null) {
            throw new IllegalArgumentException(KEY_PREFIX + "endpoints must not be empty");
        }
        return new EtcdConfigSource(endpoints, get(bootstrapArgs, "namespace", null),
                get(bootstrapArgs, "username", null), get(bootstrapArgs, "password", null),
                millis(bootstrapArgs, "connect_timeout", 3), millis(bootstrapArgs, "read_timeout", 5), httpClient, json);
    }

    /** 取 KEY_PREFIX 限定的参数并去空白；缺失或为空白时返回 defaultValue。 */
    private static String get(Map<String, String> bootstrapArgs, String key, String defaultValue) {
        String value = bootstrapArgs.get(KEY_PREFIX + key);
        if (value == null) return defaultValue;
        value = value.trim();
        return value.isEmpty() ? defaultValue : value;
    }

    private static int millis(Map<String, String> bootstrapArgs, String key, int defaultSeconds) {
        String value = get(bootstrapArgs, key, null);
        try {
            return (value == null ? defaultSeconds : Integer.parseInt(value)) * 1000;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(KEY_PREFIX + key + " is not a number: " + value);
        }
    }
}
