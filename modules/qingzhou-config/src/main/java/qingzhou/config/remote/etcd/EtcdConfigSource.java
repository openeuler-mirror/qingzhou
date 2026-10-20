package qingzhou.config.remote.etcd;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import qingzhou.common.Hosts;
import qingzhou.config.impl.Config;
import qingzhou.config.remote.RemoteConfigSource;
import qingzhou.json.Json;

/**
 * etcd v3 JSON Gateway（HTTP REST）实现：HTTP 使用 JDK 自带实现、JSON 复用框架组件，不引入第三方库。
 */
public final class EtcdConfigSource implements RemoteConfigSource {
    private final String endpoint;
    private final String prefix;
    private final String username;
    private final String password;
    private final EtcdHttp http;
    private final Json json;
    private String token;

    public EtcdConfigSource(String endpoint, String namespace, String username, String password,
                            int connectTimeout, int readTimeout, boolean allowInsecureHttp, Json json) {
        this.endpoint = endpoint.replaceAll("/+$", "").replaceAll("(?i)/v3$", "");
        validateEndpoint(this.endpoint, allowInsecureHttp);
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new IllegalArgumentException("qingzhou-config.remote.namespace must not be empty");
        }
        this.prefix = namespace.trim() + "/";
        this.username = username;
        this.password = password == null ? "" : password;
        this.http = new EtcdHttp(connectTimeout, readTimeout);
        this.json = json;
    }

    // 明文 HTTP 会把 etcd 口令与令牌暴露在网络上：非回环端点须显式确认
    private static void validateEndpoint(String endpoint, boolean allowInsecureHttp) {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid etcd endpoint: " + endpoint, e);
        }
        if (!allowInsecureHttp && "http".equalsIgnoreCase(url.getProtocol()) && !Hosts.isLoopback(url.getHost())) {
            throw new IllegalArgumentException("etcd endpoint over plaintext HTTP exposes credentials: " + endpoint
                    + "; use https, or set qingzhou-config.remote.allow_insecure_http=true to accept the risk");
        }
    }

    @Override
    public Map<String, String> pull() throws Exception {
        byte[] key = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] rangeEnd = prefixRangeEnd(key);
        String body = "{\"key\":\"" + encode(key) + "\",\"range_end\":\"" + encode(rangeEnd) + "\"}";

        Range range;
        try {
            range = range(body);
        } catch (TokenExpiredException e) {
            token = null; // etcd 令牌有 TTL：过期即重认证一次再试，避免配置拉取长期失败
            range = range(body);
        }
        Map<String, String> result = new HashMap<>();
        if (range.kvs == null) return result;

        for (Kv kv : range.kvs) {
            String pidKey = decode(kv.key);
            if (!pidKey.startsWith(prefix)) continue;// 命名空间隔离：其它命名空间的数据不可见
            String pid = pidKey.substring(prefix.length()).trim();
            if (!pid.isEmpty()) result.putAll(Config.parse(decode(kv.value)));
        }
        return result;
    }

    private Range range(String body) throws Exception {
        authenticateIfNeeded();
        EtcdHttp.Response response = http.post(endpoint + "/v3/kv/range", token, body);
        if (response.status == 401 && token != null) throw new TokenExpiredException(response.body);
        if (response.status < 200 || response.status >= 300) {
            throw new IllegalStateException("etcd /kv/range failed: http " + response.status + ", " + response.body);
        }
        return json.fromJson(response.body, Range.class);
    }

    private void authenticateIfNeeded() throws Exception {
        if (token != null || username == null || username.isEmpty()) return;

        AuthRequest auth = new AuthRequest();
        auth.name = username;
        auth.password = password;
        // 交由 Json 服务序列化，避免口令中的引号、反斜杠破坏请求体
        token = json.fromJson(post("/auth/authenticate", json.toJson(auth)), Auth.class).token;
    }

    private String post(String path, String body) throws Exception {
        EtcdHttp.Response response = http.post(endpoint + "/v3" + path, token, body);
        if (response.status < 200 || response.status >= 300) {
            throw new IllegalStateException("etcd " + path + " failed: http " + response.status + ", " + response.body);
        }
        return response.body;
    }

    // 仅供 pull 区分"令牌过期可重试"与其它失败
    private static final class TokenExpiredException extends Exception {
        TokenExpiredException(String message) {
            super(message);
        }
    }

    /**
     * 前缀扫描上界：末字节加一；0xFF 无法进位时截断向前借位，全 0xFF 时按 etcd 约定用 \x00 表示无上界
     */
    private static byte[] prefixRangeEnd(byte[] key) {
        int i = key.length - 1;
        while (i >= 0 && key[i] == (byte) 0xFF) i--;
        if (i < 0) return new byte[]{0};
        byte[] rangeEnd = Arrays.copyOf(key, i + 1);
        rangeEnd[i]++;
        return rangeEnd;
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String decode(String text) {
        return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
    }

    // etcd 响应 DTO：只声明需要的字段，int64 字段（响应中是字符串）等未知字段由 Json 实现自动忽略
    public static class Kv {
        public String key;
        public String value;
    }

    public static class Range {
        public List<Kv> kvs;
    }

    public static class Auth {
        public String token;
    }

    public static class AuthRequest {
        public String name;
        public String password;
    }
}
