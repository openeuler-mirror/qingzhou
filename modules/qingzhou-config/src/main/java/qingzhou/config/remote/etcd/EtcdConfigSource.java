package qingzhou.config.remote.etcd;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.config.remote.ConfigText;
import qingzhou.config.remote.RemoteConfigSource;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;
import qingzhou.json.Json;

/**
 * etcd v3 JSON Gateway（HTTP REST）实现：命名空间前缀下每个 key 对应一个 pid，value 为该 pid 的文档。
 * HTTP 与 JSON 均复用框架组件（qingzhou-http-client / qingzhou-json），不引入第三方库。
 */
public class EtcdConfigSource implements RemoteConfigSource {
    private final String endpoint;
    private final String prefix;
    private final String username;
    private final String password;
    private final int connectTimeout;
    private final int readTimeout;
    private final HttpClient http;
    private final Json json;
    private String token;

    public EtcdConfigSource(String endpoint, String namespace, String username, String password,
                            int connectTimeout, int readTimeout, HttpClient http, Json json) {
        this.endpoint = endpoint;
        this.prefix = (namespace == null ? "" : namespace) + "/";
        this.username = username;
        this.password = password == null ? "" : password;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.http = http;
        this.json = json;
    }

    @Override
    public Map<String, Map<String, String>> pull() throws Exception {
        byte[] key = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] rangeEnd = key.clone();
        rangeEnd[rangeEnd.length - 1]++;// 前缀扫描上界
        String body = "{\"key\":\"" + base64(key) + "\",\"range_end\":\"" + base64(rangeEnd) + "\"}";

        Range range = call("/kv/range", body, Range.class);
        Map<String, Map<String, String>> result = new HashMap<>();
        if (range.kvs == null) return result;

        for (Kv kv : range.kvs) {
            String pidKey = decode(kv.key);
            if (!pidKey.startsWith(prefix)) continue;// 命名空间隔离：其它命名空间的数据不可见
            String pid = pidKey.substring(prefix.length());
            if (!pid.isEmpty()) result.put(pid, ConfigText.parse(decode(kv.value)));
        }
        return result;
    }

    private <T> T call(String path, String body, Class<T> type) throws Exception {
        if (token == null && username != null && !username.isEmpty()) {
            AuthRequest auth = new AuthRequest();
            auth.name = username;
            auth.password = password;
            // 交由 Json 服务序列化，避免口令中的引号、反斜杠破坏请求体
            token = json.fromJson(post("/auth/authenticate", json.toJson(auth)), Auth.class).token;
        }
        return json.fromJson(post(path, body), type);
    }

    private String post(String path, String body) throws Exception {
        Request request = http.newRequest(endpoint + "/v3" + path)
                .method(HttpMethod.POST)
                .header("Content-Type", "application/json")// 覆盖客户端默认的表单类型
                .body(body.getBytes(StandardCharsets.UTF_8))
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout);
        if (token != null) request.header("Authorization", token);

        Response response = http.send(request);
        String text = new String(response.getBody(), StandardCharsets.UTF_8);
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            throw new IllegalStateException("etcd " + path + " failed: http " + response.getStatus() + ", " + text);
        }
        return text;
    }

    private static String base64(byte[] bytes) {
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
