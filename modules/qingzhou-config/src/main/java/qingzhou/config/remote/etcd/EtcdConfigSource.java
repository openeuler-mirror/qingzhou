package qingzhou.config.remote.etcd;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import qingzhou.config.remote.RemoteConfigSource;
import qingzhou.config.remote.RemoteOptions;

/**
 * etcd v3 JSON Gateway（HTTP REST）实现，不引入第三方库。
 * 命名空间前缀下的每个 key 对应一个 pid，value 为 pid 内部 key=value 文本。
 */
public class EtcdConfigSource implements RemoteConfigSource {
    private final String[] endpoints;
    private final String username;
    private final String password;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private String token;

    public EtcdConfigSource(RemoteOptions options) {
        List<String> list = new ArrayList<>();
        if (options.endpoints != null) {
            for (String ep : options.endpoints.split(",")) {
                String endpoint = normalizeEndpoint(ep);
                if (!endpoint.isEmpty()) list.add(endpoint);
            }
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("qingzhou-config.remote.endpoints must not be empty");
        }
        this.endpoints = list.toArray(new String[0]);
        this.username = options.username == null ? "" : options.username.trim();
        this.password = options.password == null ? "" : options.password;
        this.connectTimeoutMs = options.connectTimeoutMs;
        this.readTimeoutMs = options.readTimeoutMs;
    }

    private static String normalizeEndpoint(String endpoint) {
        String e = endpoint.trim();
        while (e.endsWith("/")) {
            e = e.substring(0, e.length() - 1);
        }
        if (e.toLowerCase(Locale.ROOT).endsWith("/v3")) {
            e = e.substring(0, e.length() - 3);
        }
        return e;
    }

    @Override
    public Map<String, Map<String, String>> pull(String namespace) throws Exception {
        Exception last = null;
        String prefix = namespace + "/";
        for (String endpoint : endpoints) {
            try {
                return range(endpoint, prefix);
            } catch (Exception e) {
                last = e;
            }
        }
        if (last != null) {
            throw last;
        }
        return Collections.emptyMap();
    }

    private Map<String, Map<String, String>> range(String endpoint, String prefix) throws Exception {
        String auth = token(endpoint);
        byte[] key = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] rangeEnd = key.clone();
        rangeEnd[rangeEnd.length - 1] = (byte) (rangeEnd[rangeEnd.length - 1] + 1); // 前缀扫描上界

        String body = "{\"key\":\"" + base64(key) + "\",\"range_end\":\"" + base64(rangeEnd) + "\"}";
        String response = httpPost(endpoint, "/kv/range", body, auth);
        return parseRange(response, prefix);
    }

    private Map<String, Map<String, String>> parseRange(String response, String prefix) throws IOException {
        Map<String, Map<String, String>> result = new HashMap<>();
        Object root = parseJson(response, "range");
        if (!(root instanceof Map)) {
            throw new IllegalStateException("etcd range response is not an object");
        }
        Object kvs = ((Map<?, ?>) root).get("kvs");
        if (!(kvs instanceof List)) {
            return result;
        }
        for (Object item : (List<?>) kvs) {
            if (!(item instanceof Map)) {
                throw new IllegalStateException("etcd range response has an invalid kv item");
            }
            Map<?, ?> kv = (Map<?, ?>) item;
            String key = decodeBase64(asText(kv.get("key"), "range"));
            if (!key.startsWith(prefix)) {
                continue;
            }
            String pid = key.substring(prefix.length());
            if (pid.isEmpty()) {
                continue;
            }
            String value = decodeBase64(asText(kv.get("value"), "range"));
            result.put(pid, parseDocument(pid, value));
        }
        return result;
    }

    private Map<String, String> parseDocument(String pid, String value) throws IOException {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(value))) {
            StringBuilder pending = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) {
                line = line.replaceAll("^[\\s　]+", "");
                if (line.isEmpty() || line.startsWith("#") || line.equals("\\")) continue;
                if (line.endsWith("\\")) {
                    pending.append(line, 0, line.length() - 1);
                    continue;
                }
                pending.append(line);
                String target = pending.toString();
                pending.setLength(0);
                int i = target.indexOf('=');
                if (i > 0) {
                    String key = target.substring(0, i).trim();
                    if (!key.isEmpty()) map.put(key, target.substring(i + 1).trim());
                } else if (!target.isEmpty()) {
                    map.put(target, "");
                }
            }
        }
        return map;
    }

    private String token(String endpoint) throws Exception {
        if (username.isEmpty()) return null;
        if (token != null) return token;

        String body = "{\"name\":\"" + escape(username) + "\",\"password\":\"" + escape(password) + "\"}";
        Object root = parseJson(httpPost(endpoint, "/auth/authenticate", body, null), "authenticate");
        if (!(root instanceof Map)) {
            throw new IllegalStateException("etcd authenticate response is not an object");
        }
        Object t = ((Map<?, ?>) root).get("token");
        if (!(t instanceof String) || ((String) t).isEmpty()) {
            throw new IllegalStateException("etcd authenticate response has no token");
        }
        token = (String) t;
        return token;
    }

    private String httpPost(String endpoint, String path, String json, String auth) throws Exception {
        String url = endpoint + "/v3" + path;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            if (auth != null) {
                conn.setRequestProperty("Authorization", auth);
            }
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            try (OutputStream out = conn.getOutputStream()) {
                out.write(bytes);
            }

            int code = conn.getResponseCode();
            boolean success = code >= 200 && code < 300;
            String response = readAll(success ? conn.getInputStream() : conn.getErrorStream());
            if (!success) {
                throw new IllegalStateException("etcd request failed: http " + code + ", url: " + url + ", reason: " + errorMessage(response));
            }
            return response;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String errorMessage(String response) {
        if (response == null || response.isEmpty()) return "unknown";
        try {
            Object root = Json.parse(response);
            if (root instanceof Map) {
                Object message = ((Map<?, ?>) root).get("message");
                if (message == null) message = ((Map<?, ?>) root).get("error");
                if (message instanceof String && !((String) message).isEmpty()) {
                    return (String) message;
                }
            }
        } catch (RuntimeException ignored) {
            // 非 JSON 错误体，直接截取
        }
        String body = response.replaceAll("[\\r\\n\\t]+", " ").trim();
        return body.length() > 120 ? body.substring(0, 120) + "..." : body;
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[8192];
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            for (int read; (read = reader.read(buffer)) != -1; ) {
                sb.append(buffer, 0, read);
            }
        }
        return sb.toString();
    }

    private Object parseJson(String text, String action) {
        try {
            return Json.parse(text);
        } catch (RuntimeException e) {
            throw new IllegalStateException("invalid etcd " + action + " response", e);
        }
    }

    private static String asText(Object value, String action) {
        if (!(value instanceof String)) {
            throw new IllegalStateException("etcd " + action + " response misses a string field");
        }
        return (String) value;
    }

    private static String decodeBase64(String text) {
        try {
            return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            throw new IllegalStateException("invalid base64 content in etcd response", e);
        }
    }

    private static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String escape(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
