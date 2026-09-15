package qingzhou.config.remote;

import java.util.Properties;

import qingzhou.config.remote.etcd.EtcdConfigSource;
import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;

/** 按 qingzhou-config.remote.* 自举参数创建远程配置源；是否启用由调用方先判断。 */
public final class RemoteConfigSourceFactory {
    public static final String KEY_PREFIX = "qingzhou-config.remote.";

    private RemoteConfigSourceFactory() {
    }

    public static RemoteConfigSource create(Properties config, HttpClient httpClient, Json json) {
        String type = get(config, "type");
        if (type == null || type.trim().isEmpty()) type = "etcd";
        if (!"etcd".equalsIgnoreCase(type.trim())) {
            throw new UnsupportedOperationException("unsupported remote config center type: " + type);
        }
        if (httpClient == null || json == null) {
            throw new IllegalStateException("HttpClient/Json service is required when remote config center is enabled");
        }
        return new EtcdConfigSource(get(config, "endpoints"), get(config, "namespace"),
                get(config, "username"), get(config, "password"),
                seconds(config, "connect_timeout", 3), seconds(config, "read_timeout", 5), httpClient, json);
    }

    private static String get(Properties config, String key) {
        String value = config.getProperty(KEY_PREFIX + key);
        return value == null ? null : value.trim();
    }

    /** 配置单位为秒，HttpClient 需要毫秒 */
    private static int seconds(Properties config, String key, int defaultValue) {
        String value = get(config, key);
        return (value == null ? defaultValue : Integer.parseInt(value)) * 1000;
    }
}
