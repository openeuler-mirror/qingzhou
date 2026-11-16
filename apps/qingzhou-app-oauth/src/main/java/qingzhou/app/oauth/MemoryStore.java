package qingzhou.app.oauth;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import qingzhou.crypto.Crypto;
import qingzhou.crypto.MessageDigest;

/**
 * 内存存储：默认模式，进程重启即清空；适合单实例、试用或不需要持久化的部署。
 */
final class MemoryStore extends Store {
    private final Map<String, Map<String, String>> clients = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> users = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> codes = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> tokens = new ConcurrentHashMap<>();

    MemoryStore(MessageDigest messageDigest, Crypto crypto, Map<String, String> config) {
        super(messageDigest, crypto, config);
    }

    @Override
    void init() {
    }

    @Override
    void cleanup() {
        codes.entrySet().removeIf(entry -> {
            Map<String, String> code = entry.getValue();
            return "1".equals(code.get("used")) || expired(code, "expires_at");
        });
        tokens.entrySet().removeIf(entry -> {
            Map<String, String> token = entry.getValue();
            boolean accessExpired = expired(token, "expires_at");
            String refreshExpires = token.get("refresh_expires_at");
            boolean refreshExpired = Security.isEmpty(refreshExpires)
                    || "0".equals(refreshExpires) || expired(refreshExpires);
            return accessExpired && refreshExpired;
        });
    }

    @Override
    String storageMode() {
        return "memory";
    }

    @Override
    Map<String, String> findClient(String clientId) {
        for (Map<String, String> client : clients.values()) {
            if (clientId != null && clientId.equals(client.get("client_id"))) return copy(client);
        }
        return null;
    }

    @Override
    Map<String, String> findUser(String userName) {
        for (Map<String, String> user : users.values()) {
            if (userName != null && userName.equals(user.get("username"))) return copy(user);
        }
        return null;
    }

    @Override
    void saveAuthCode(Map<String, String> authCode) {
        codes.put(authCode.get("id"), copy(authCode));
    }

    @Override
    Map<String, String> findUsableAuthCode(String code, String clientId) {
        for (Map<String, String> row : codes.values()) {
            if (code != null && code.equals(row.get("code"))
                    && clientId != null && clientId.equals(row.get("client_id"))
                    && !"1".equals(row.get("used")) && !expired(row, "expires_at")) {
                return copy(row);
            }
        }
        return null;
    }

    @Override
    synchronized boolean consumeAuthCode(String id) {
        Map<String, String> row = codes.get(id);
        if (row == null || "1".equals(row.get("used")) || expired(row, "expires_at")) return false;
        row.put("used", "1");
        return true;
    }

    @Override
    void saveToken(Map<String, String> token) {
        tokens.put(token.get("id"), copy(token));
    }

    @Override
    Map<String, String> findValidAccessToken(String accessToken) {
        for (Map<String, String> token : tokens.values()) {
            if (accessToken != null && accessToken.equals(token.get("access_token"))
                    && !expired(token, "expires_at")) {
                return copy(token);
            }
        }
        return null;
    }

    @Override
    Map<String, String> findValidRefreshToken(String refreshToken, String clientId) {
        for (Map<String, String> token : tokens.values()) {
            if (refreshToken != null && refreshToken.equals(token.get("refresh_token"))
                    && clientId != null && clientId.equals(token.get("client_id"))
                    && !expired(token, "refresh_expires_at")) {
                return copy(token);
            }
        }
        return null;
    }

    @Override
    Map<String, String> findToken(String token) {
        for (Map<String, String> row : tokens.values()) {
            if (token != null && (token.equals(row.get("access_token")) || token.equals(row.get("refresh_token")))) {
                return copy(row);
            }
        }
        return null;
    }

    @Override
    boolean deleteToken(String id) {
        return tokens.remove(id) != null;
    }

    @Override
    List<String[]> page(String table, String searchColumn, int pageNum, int pageSize,
                        String keyword, String[] listFields) {
        List<Map<String, String>> rows = matching(table, searchColumn, keyword);
        rows.sort(Comparator.comparing(row -> row.getOrDefault("id", "")));
        int size = Math.max(1, Math.min(pageSize, 100));
        int from = Math.max(0, (Math.max(pageNum, 1) - 1) * size);
        List<String[]> result = new ArrayList<>();
        for (int i = from; i < Math.min(from + size, rows.size()); i++) {
            Map<String, String> row = rows.get(i);
            String[] data = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                data[j] = row.getOrDefault(listFields[j].toLowerCase(Locale.ROOT), "");
            }
            result.add(data);
        }
        return result;
    }

    @Override
    int count(String table, String searchColumn, String keyword) {
        return matching(table, searchColumn, keyword).size();
    }

    @Override
    Map<String, String> show(String table, String id) {
        Map<String, String> row = rows(table).get(id);
        return row == null ? null : copy(row);
    }

    @Override
    void insertRow(String table, Map<String, String> row) {
        rows(table).put(row.get("id"), copy(row));
    }

    @Override
    void updateRow(String table, String id, Map<String, String> row) {
        Map<String, String> existing = rows(table).get(id);
        if (existing == null) return;
        existing.putAll(row);
        existing.put("id", id);
    }

    @Override
    void deleteRow(String table, String id) {
        rows(table).remove(id);
    }

    @Override
    void deleteBy(String table, String column, String value) {
        if (value == null) return;
        rows(table).entrySet().removeIf(entry -> value.equals(entry.getValue().get(column)));
    }

    private List<Map<String, String>> matching(String table, String searchColumn, String keyword) {
        List<Map<String, String>> result = new ArrayList<>();
        String search = Security.isEmpty(keyword) ? null : keyword.toLowerCase(Locale.ROOT);
        for (Map<String, String> row : rows(table).values()) {
            if (search != null) {
                String value = row.getOrDefault(searchColumn.toLowerCase(Locale.ROOT), "");
                if (!value.toLowerCase(Locale.ROOT).contains(search)) continue;
            }
            result.add(copy(row));
        }
        return result;
    }

    private Map<String, Map<String, String>> rows(String table) {
        if (CLIENT_TABLE.equals(table)) return clients;
        if (USER_TABLE.equals(table)) return users;
        if (AUTH_CODE_TABLE.equals(table)) return codes;
        if (TOKEN_TABLE.equals(table)) return tokens;
        throw new IllegalArgumentException("非法表名: " + table);
    }

    private static Map<String, String> copy(Map<String, String> row) {
        return new LinkedHashMap<>(row);
    }
}
