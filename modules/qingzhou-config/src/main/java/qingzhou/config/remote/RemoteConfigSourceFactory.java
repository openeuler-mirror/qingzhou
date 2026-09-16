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

    private RemoteConfigSourceFactory() {
    }

    public static RemoteConfigSource create(Map<String, String> remoteRequestArgs, HttpClient httpClient, Json json) {
        String type = get(remoteRequestArgs, "type");
        if (type == null || type.trim().isEmpty()) type = "etcd";
        if (!"etcd".equalsIgnoreCase(type.trim())) {
            throw new UnsupportedOperationException("unsupported remote config center type: " + type);
        }
        if (httpClient == null || json == null) {
            throw new IllegalStateException("HttpClient/Json service is required when remote config center is enabled");
        }
        return new EtcdConfigSource(get(remoteRequestArgs, "endpoints"), get(remoteRequestArgs, "namespace"),
                get(remoteRequestArgs, "username"), get(remoteRequestArgs, "password"),
                seconds(remoteRequestArgs, "connect_timeout", 3), seconds(remoteRequestArgs, "read_timeout", 5), httpClient, json);
    }

    private static String get(Map<String, String> config, String key) {
        String value = config.get(KEY_PREFIX + key);
        return value == null ? null : value.trim();
    }

    /**
     * 配置单位为秒，HttpClient 需要毫秒
     */
    private static int seconds(Map<String, String> config, String key, int defaultValue) {
        String value = get(config, key);
        return (value == null ? defaultValue : Integer.parseInt(value)) * 1000;
    }
}
