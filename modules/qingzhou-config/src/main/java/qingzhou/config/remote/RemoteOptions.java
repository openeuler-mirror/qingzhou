package qingzhou.config.remote;

import java.util.Properties;

/**
 * 外部配置中心自举参数，对应 qingzhou.properties 中 qingzhou-config.remote.* 键。
 * 命名空间按 服务/部署/租户/实例 等维度拼接，实现对共享配置中心的逻辑隔离。
 */
public class RemoteOptions {
    public static final String KEY_PREFIX = "qingzhou-config.remote.";
    private static final String SEGMENT_REGEX = "[A-Za-z0-9._-]+";

    public final boolean enabled;
    public final String type;
    public final String endpoints;
    public final String username;
    public final String password;
    public final int connectTimeoutMs;
    public final int readTimeoutMs;
    private final String namespace; // 显式命名空间，为空时按各维度拼接

    public final String service;
    public final String deployment;
    public final String tenant;
    public final String instance;

    private RemoteOptions(boolean enabled, String type, String endpoints, String username, String password,
                          int connectTimeoutMs, int readTimeoutMs, String namespace,
                          String service, String deployment, String tenant, String instance) {
        this.enabled = enabled;
        this.type = type;
        this.endpoints = endpoints;
        this.username = username;
        this.password = password;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.namespace = namespace;
        this.service = service;
        this.deployment = deployment;
        this.tenant = tenant;
        this.instance = instance;
    }

    /** 从 qingzhou.properties 解析自举参数；缺省值：不启用、类型 etcd、维度全 default。 */
    public static RemoteOptions from(Properties qzConfig) {
        String instanceName = System.getProperty("qingzhou.instance");
        if (instanceName != null && !instanceName.trim().isEmpty()) {
            int idx = Math.max(instanceName.replace('\\', '/').lastIndexOf('/'), 0);
            String name = instanceName.replace('\\', '/').substring(idx);
            if (name.startsWith("/")) name = name.substring(1);
            if (!name.trim().isEmpty()) instanceName = name;
        }
        return new RemoteOptions(
                getBool(qzConfig, "enabled", false),
                get(qzConfig, "type", "etcd"),
                get(qzConfig, "endpoints", ""),
                get(qzConfig, "username", null),
                get(qzConfig, "password", null),
                getInt(qzConfig, "connect_timeout", 3) * 1000,
                getInt(qzConfig, "read_timeout", 5) * 1000,
                get(qzConfig, "namespace", null),
                get(qzConfig, "service", "qingzhou"),
                get(qzConfig, "deployment", "default"),
                get(qzConfig, "tenant", "default"),
                get(qzConfig, "instance", instanceName != null ? instanceName : "default"));
    }

    /** 构造隔离命名空间；显式 namespace 优先生效。 */
    public String buildNamespace() {
        String explicit = trimToNull(namespace);
        if (explicit != null) return checkSegments(explicit, '/');

        return checkSegments(checkSegment(service) + "/" + checkSegment(deployment)
                + "/" + checkSegment(tenant) + "/" + checkSegment(instance), null);
    }

    private String checkSegment(String value) {
        String v = trimToNull(value);
        if (v == null) {
            throw new IllegalArgumentException("remote config namespace segment must not be empty");
        }
        if (!v.matches(SEGMENT_REGEX)) {
            throw new IllegalArgumentException("invalid remote config namespace segment: " + v);
        }
        return v;
    }

    private String checkSegments(String path, Character separator) {
        String p = path;
        if (separator != null) {
            while (p.startsWith("/")) p = p.substring(1);
            while (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        }
        StringBuilder sb = new StringBuilder();
        for (String segment : p.split("/")) {
            sb.append(checkSegment(segment)).append('/');
        }
        return sb.substring(0, sb.length() - 1);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static String get(Properties props, String key, String defaultValue) {
        String value = props.getProperty(KEY_PREFIX + key);
        return value == null ? defaultValue : value.trim();
    }

    private static boolean getBool(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(KEY_PREFIX + key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    private static int getInt(Properties props, String key, int defaultValue) {
        String value = props.getProperty(KEY_PREFIX + key);
        if (value == null) return defaultValue;
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
