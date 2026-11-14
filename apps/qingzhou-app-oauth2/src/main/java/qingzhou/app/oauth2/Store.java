package qingzhou.app.oauth2;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

import qingzhou.api.AppContext;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.MessageDigest;

/**
 * OAuth 2.0 数据存取抽象：内存与 JDBC 两种实现共享令牌签发、摘要校验与种子数据逻辑。
 */
public abstract class Store {
    static final String CLIENT_TABLE = "oauth_client";
    static final String USER_TABLE = "oauth_user";
    static final String AUTH_CODE_TABLE = "oauth_auth_code";
    static final String TOKEN_TABLE = "oauth_token";

    static final String DEFAULT_GRANT_TYPES = "authorization_code,password,client_credentials,refresh_token";
    static final String DEFAULT_AUTH_METHOD = "client_secret_basic";
    static final String TOKEN_TYPE = "Bearer";

    private static final long DEFAULT_ACCESS_TOKEN_TTL = 7200;
    private static final long DEFAULT_REFRESH_TOKEN_TTL = 604800;
    private static final long DEFAULT_AUTH_CODE_TTL = 600;

    private static final Map<String, Set<String>> TABLE_COLUMNS = new LinkedHashMap<>();
    private static final Set<String> SECRET_COLUMNS = new HashSet<>(Arrays.asList("client_secret", "password"));

    static {
        TABLE_COLUMNS.put(CLIENT_TABLE, setOf("id", "client_id", "client_secret", "client_name", "redirect_uri",
                "grant_types", "scope", "token_endpoint_auth_method", "require_pkce",
                "access_token_ttl", "refresh_token_ttl", "enabled", "create_time"));
        TABLE_COLUMNS.put(USER_TABLE, setOf("id", "username", "password", "nickname", "email", "rolename", "enabled", "create_time"));
        TABLE_COLUMNS.put(AUTH_CODE_TABLE, setOf("id", "code", "client_id", "username", "redirect_uri", "scope",
                "code_challenge", "code_challenge_method", "nonce", "auth_time",
                "expires_at", "used", "create_time"));
        TABLE_COLUMNS.put(TOKEN_TABLE, setOf("id", "access_token", "refresh_token", "client_id", "username",
                "token_type", "scope", "expires_at", "refresh_expires_at", "create_time"));
    }

    private static volatile Store instance;

    final MessageDigest messageDigest;
    final Crypto crypto;
    final long accessTokenTtl;
    final long refreshTokenTtl;
    final long authCodeTtl;
    final String defaultScope;
    final String issuer;

    Store(MessageDigest messageDigest, Crypto crypto, Map<String, String> config) {
        this.messageDigest = messageDigest;
        this.crypto = crypto;
        this.accessTokenTtl = positive(config.get("access_token_ttl"), DEFAULT_ACCESS_TOKEN_TTL);
        this.refreshTokenTtl = positive(config.get("refresh_token_ttl"), DEFAULT_REFRESH_TOKEN_TTL);
        this.authCodeTtl = positive(config.get("auth_code_ttl"), DEFAULT_AUTH_CODE_TTL);
        String scope = config.get("default_scope");
        this.defaultScope = Security.isEmpty(scope) ? "read" : scope.trim();
        String configuredIssuer = config.get("issuer");
        this.issuer = configuredIssuer == null ? "" : configuredIssuer.trim();
    }

    /**
     * 应用与模型共享同一 Store 实例；storage 默认 memory，配置 jdbc 时切换。
     */
    public static Store get(AppContext appContext) throws Exception {
        if (instance != null) return instance;
        synchronized (Store.class) {
            if (instance != null) return instance;

            Map<String, String> config = appContext.getAppConfig();
            Crypto crypto = appContext.getService(Crypto.class);
            if (crypto == null) throw new IllegalStateException("Crypto 不可用，无法校验客户端密钥与用户口令");

            String mode = config.getOrDefault("storage", "memory").trim().toLowerCase(Locale.ROOT);
            Store store;
            if ("memory".equals(mode)) {
                store = new MemoryStore(crypto.getMessageDigest(), crypto, config);
            } else if ("jdbc".equals(mode)) {
                store = JdbcStore.create(appContext, crypto.getMessageDigest(), crypto, config);
            } else {
                throw new IllegalArgumentException("不支持的 storage: " + mode + "（可选 memory / jdbc）");
            }

            store.init();
            if (Boolean.parseBoolean(config.getOrDefault("seed_demo", "false"))) store.seed();
            instance = store;
            return store;
        }
    }

    abstract void init() throws Exception;

    abstract void cleanup() throws SQLException;

    abstract String storageMode();

    abstract Map<String, String> findClient(String clientId) throws SQLException;

    abstract Map<String, String> findUser(String userName) throws SQLException;

    abstract void saveAuthCode(Map<String, String> authCode) throws SQLException;

    abstract Map<String, String> findUsableAuthCode(String code, String clientId) throws SQLException;

    abstract boolean consumeAuthCode(String id) throws SQLException;

    abstract void saveToken(Map<String, String> token) throws SQLException;

    abstract Map<String, String> findValidAccessToken(String accessToken) throws SQLException;

    abstract Map<String, String> findValidRefreshToken(String refreshToken, String clientId) throws SQLException;

    abstract Map<String, String> findToken(String token) throws SQLException;

    abstract boolean deleteToken(String id) throws SQLException;

    abstract List<String[]> page(String table, String searchColumn, int pageNum, int pageSize,
                                 String keyword, String[] listFields) throws SQLException;

    abstract int count(String table, String searchColumn, String keyword) throws SQLException;

    abstract Map<String, String> show(String table, String id) throws SQLException;

    abstract void insertRow(String table, Map<String, String> row) throws SQLException;

    abstract void updateRow(String table, String id, Map<String, String> row) throws SQLException;

    abstract void deleteRow(String table, String id) throws SQLException;

    abstract void deleteBy(String table, String column, String value) throws SQLException;

    final boolean verifySecret(String provided, String stored) {
        if (Security.isEmpty(provided) || Security.isEmpty(stored)) return false;

        // 只有形如 algorithm$salt$iterations$digest 的值才进入 PBKDF2；
        // 历史/外部数据可能是平台 AES 密文或明文，不能让它们被误当作摘要做高成本计算。
        if (looksLikeDigest(stored)) {
            try {
                if (messageDigest.matches(provided, stored)) return true;
            } catch (RuntimeException ignored) {
            }
        } else {
            try {
                String plain = crypto.getGlobalCipher().decrypt(stored);
                if (plain != null && constantTimeEquals(provided, plain)) return true;
            } catch (Exception ignored) {
            }
        }
        return constantTimeEquals(provided, stored);
    }

    private static boolean looksLikeDigest(String value) {
        String[] parts = value.split("\\$", -1);
        if (parts.length != 4) return false;
        if (!parts[0].startsWith("PBKDF2") && !parts[0].startsWith("SHA-")) return false;
        try {
            Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return false;
        }
        return parts[1].matches("[0-9A-Fa-f]*") && parts[3].matches("[0-9A-Fa-f]+");
    }

    private static boolean constantTimeEquals(String provided, String stored) {
        return java.security.MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
    }

    final boolean hasUsers() throws SQLException {
        return count(USER_TABLE, "username", "") > 0;
    }

    final boolean verifyPassword(String userName, String password) throws SQLException {
        Map<String, String> user = findUser(userName);
        return user != null && isEnabled(user) && verifySecret(password, user.get("password"));
    }

    /**
     * 签发令牌；client_credentials 与 implicit 不签发 refresh_token。
     */
    final Map<String, Object> issueToken(String clientId, String userName, String scope,
                                         boolean includeRefresh) throws SQLException {
        Map<String, String> client = findClient(clientId);
        long accessTtl = accessTtl(client);
        long refreshTtl = positive(client == null ? null : client.get("refresh_token_ttl"), refreshTokenTtl);

        String grantedScope = Security.isEmpty(scope) ? defaultScope : scope;
        // 只有客户端登记 refresh_token 授予时才签发刷新令牌，避免返回无法使用的凭据
        boolean withRefresh = includeRefresh && client != null && Security.grantAllowed(client, "refresh_token");
        String accessToken = Security.randomToken(32);
        String refreshToken = withRefresh ? Security.randomToken(32) : null;
        long now = now();

        Map<String, String> row = new LinkedHashMap<>();
        row.put("id", UUID.randomUUID().toString());
        row.put("access_token", accessToken);
        row.put("refresh_token", refreshToken);
        row.put("client_id", clientId);
        row.put("username", userName == null ? "" : userName);
        row.put("token_type", TOKEN_TYPE);
        row.put("scope", grantedScope);
        row.put("expires_at", String.valueOf(now + accessTtl));
        row.put("refresh_expires_at", refreshToken == null ? "0" : String.valueOf(now + refreshTtl));
        row.put("create_time", String.valueOf(now));
        saveToken(row);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", TOKEN_TYPE);
        response.put("expires_in", accessTtl);
        if (refreshToken != null) response.put("refresh_token", refreshToken);
        response.put("scope", grantedScope);
        return response;
    }

    final void insert(String table, Map<String, String> data) throws SQLException {
        Map<String, String> row = canonicalRow(table, data);
        row.put("id", UUID.randomUUID().toString());
        row.put("create_time", String.valueOf(now()));
        applyDefaults(table, row);
        hashSecrets(row);
        insertRow(table, row);
    }

    final void update(String table, String id, Map<String, String> data) throws SQLException {
        Map<String, String> row = canonicalRow(table, data);
        row.remove("id");
        row.remove("create_time");
        if (row.isEmpty()) return;
        hashSecrets(row);
        updateRow(table, id, row);
    }

    final void delete(String table, String id) throws SQLException {
        Map<String, String> row = show(table, id);
        if (row == null) return;
        deleteRow(table, id);

        if (CLIENT_TABLE.equals(table)) {
            deleteBy(AUTH_CODE_TABLE, "client_id", row.get("client_id"));
            deleteBy(TOKEN_TABLE, "client_id", row.get("client_id"));
        } else if (USER_TABLE.equals(table)) {
            deleteBy(AUTH_CODE_TABLE, "username", row.get("username"));
            deleteBy(TOKEN_TABLE, "username", row.get("username"));
        }
    }

    final boolean exists(String table, String id) throws SQLException {
        return show(table, id) != null;
    }

    final long accessTtl(Map<String, String> client) {
        return positive(client == null ? null : client.get("access_token_ttl"), accessTokenTtl);
    }

    final String getIssuer() {
        return issuer;
    }

    final String getDefaultScope() {
        return defaultScope;
    }

    private void seed() throws SQLException {
        if (findClient("test_client") != null) return;

        insert(CLIENT_TABLE, row("client_id", "test_client", "client_secret", "test_secret",
                "client_name", "测试客户端", "redirect_uri", "https://localhost:7900/oauth2/callback",
                "grant_types", DEFAULT_GRANT_TYPES, "scope", "openid profile email read write",
                "token_endpoint_auth_method", DEFAULT_AUTH_METHOD, "enabled", "true"));
        insert(USER_TABLE, row("username", "admin", "password", "admin123",
                "nickname", "管理员", "email", "admin@example.com", "rolename", "system", "enabled", "true"));
        insert(USER_TABLE, row("username", "test", "password", "test123",
                "nickname", "测试用户", "email", "test@example.com", "rolename", "auditor", "enabled", "true"));
    }

    private Map<String, String> canonicalRow(String table, Map<String, String> data) {
        Set<String> columns = columns(table);
        Map<String, String> row = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            if (!columns.contains(key)) throw new IllegalArgumentException("非法字段: " + entry.getKey());
            row.put(key, entry.getValue());
        }
        return row;
    }

    private void applyDefaults(String table, Map<String, String> row) {
        if (CLIENT_TABLE.equals(table)) {
            putIfEmpty(row, "grant_types", DEFAULT_GRANT_TYPES);
            putIfEmpty(row, "scope", defaultScope);
            putIfEmpty(row, "token_endpoint_auth_method", DEFAULT_AUTH_METHOD);
            putIfEmpty(row, "require_pkce", "false");
            putIfEmpty(row, "enabled", "true");
        } else if (USER_TABLE.equals(table)) {
            putIfEmpty(row, "enabled", "true");
        } else if (TOKEN_TABLE.equals(table)) {
            putIfEmpty(row, "token_type", TOKEN_TYPE);
        }
    }

    private void hashSecrets(Map<String, String> row) {
        for (String column : SECRET_COLUMNS) {
            String value = row.get(column);
            if (!Security.isEmpty(value)) row.put(column, messageDigest.digest(value));
        }
    }

    static Set<String> columns(String table) {
        Set<String> columns = TABLE_COLUMNS.get(table);
        if (columns == null) throw new IllegalArgumentException("非法表名: " + table);
        return columns;
    }

    private static void putIfEmpty(Map<String, String> row, String key, String value) {
        String current = row.get(key);
        if (Security.isEmpty(current)) row.put(key, value);
    }

    static boolean isEnabled(Map<String, String> row) {
        if (row == null) return false;
        String value = row.get("enabled");
        return Security.isEmpty(value) || "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    static boolean expired(Map<String, String> row, String key) {
        if (row == null) return true;
        return expired(row.get(key));
    }

    static boolean expired(String expiresAt) {
        if (Security.isEmpty(expiresAt)) return true;
        try {
            return Long.parseLong(expiresAt) < now();
        } catch (NumberFormatException e) {
            return true;
        }
    }

    static long now() {
        return System.currentTimeMillis() / 1000;
    }

    static Map<String, String> row(String... keyValues) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) row.put(keyValues[i], keyValues[i + 1]);
        return row;
    }

    private static long positive(String value, long defaultValue) {
        if (!Security.isEmpty(value)) {
            try {
                long parsed = Long.parseLong(value.trim());
                if (parsed > 0) return parsed;
            } catch (NumberFormatException ignored) {
                // 配置非法时用默认值，避免启动失败
            }
        }
        return defaultValue;
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}
