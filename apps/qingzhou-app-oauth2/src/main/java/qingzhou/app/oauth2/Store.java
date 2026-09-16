package qingzhou.app.oauth2;

import java.sql.*;
import java.util.*;

import qingzhou.api.AppContext;
import qingzhou.jdbc.JdbcPool;

/**
 * OAuth 2.0 数据存取：客户端、用户、授权码与令牌。
 */
public final class Store {
    static final String CLIENT_TABLE = "oauth_client";
    static final String USER_TABLE = "oauth_user";

    static final long ACCESS_TOKEN_EXPIRE = 7200;
    private static final long REFRESH_TOKEN_EXPIRE = 604800;
    private static final long AUTH_CODE_EXPIRE = 600;
    private static final String DEFAULT_SCOPE = "read";
    private static final String TOKEN_TYPE = "bearer";

    private static final String[] SCHEMA = {
            "CREATE TABLE IF NOT EXISTS oauth_client (id IDENTITY PRIMARY KEY, client_id VARCHAR(64) NOT NULL UNIQUE, client_secret VARCHAR(128) NOT NULL, client_name VARCHAR(128) NOT NULL, redirect_uri VARCHAR(256), grant_types VARCHAR(256) DEFAULT 'authorization_code,password,client_credentials,refresh_token', scope VARCHAR(256) DEFAULT 'read write', create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS oauth_user (id IDENTITY PRIMARY KEY, userName VARCHAR(64) NOT NULL UNIQUE, password VARCHAR(128) NOT NULL, nickname VARCHAR(128), roleName VARCHAR(128), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS oauth_auth_code (id IDENTITY PRIMARY KEY, code VARCHAR(128) NOT NULL UNIQUE, client_id VARCHAR(64) NOT NULL, username VARCHAR(64) NOT NULL, redirect_uri VARCHAR(256), scope VARCHAR(256), expires_at BIGINT NOT NULL, used TINYINT DEFAULT 0, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS oauth_token (id IDENTITY PRIMARY KEY, access_token VARCHAR(256) NOT NULL UNIQUE, refresh_token VARCHAR(256), client_id VARCHAR(64) NOT NULL, userName VARCHAR(64), token_type VARCHAR(32) DEFAULT 'bearer', scope VARCHAR(256), expires_at BIGINT NOT NULL, refresh_expires_at BIGINT, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
    };

    private static volatile Store instance;

    private final JdbcPool jdbcPool;

    private Store(JdbcPool jdbcPool) {
        this.jdbcPool = jdbcPool;
    }

    /**
     * 首次访问时建表并写入种子数据。应用与模型均由 AppContext 共享同一实例。
     * <p>
     * 数据源按名取自平台 JdbcPool 服务，名字即 qingzhou.properties 中
     * {@code qingzhou-jdbc~<name>.*} 里 {@code ~} 之后的名字，可用
     * {@code app~qingzhou-app-oauth2.jdbc_name} 指定（默认 h2），
     * 因此具体使用 h2、oracle 还是其它实现由平台配置决定，应用只依赖 JdbcPool API。
     */
    public static Store get(AppContext appContext) throws SQLException {
        if (instance == null) {
            synchronized (Store.class) {
                if (instance == null) {
                    String name = appContext.getProperties().getProperty("jdbc_name", "h2");
                    JdbcPool jdbcPool = appContext.getService(JdbcPool.class, name);
                    if (jdbcPool == null) {
                        return null;
                    }
                    Store store = new Store(jdbcPool);
                    store.init();
                    instance = store;
                }
            }
        }
        return instance;
    }

    private void init() throws SQLException {
        try (Connection connection = jdbcPool.getConnection();
             Statement statement = connection.createStatement()) {
            for (String ddl : SCHEMA) {
                statement.execute(ddl);
            }
        }
        seed();
    }

    private void seed() throws SQLException {
        if (findClient("test_client") != null) return;

        insert(CLIENT_TABLE, row("client_id", "test_client", "client_secret", "test_secret",
                "client_name", "测试客户端", "redirect_uri", "https://localhost:7900/oauth2/callback"));
        insert(USER_TABLE, row("userName", "admin", "password", "admin123",
                "nickname", "管理员", "roleName", "system"));
        insert(USER_TABLE, row("userName", "test", "password", "test123",
                "nickname", "测试用户", "roleName", "auditor"));
    }

    Map<String, String> findClient(String clientId) throws SQLException {
        return queryOne("SELECT * FROM oauth_client WHERE client_id = ?", clientId);
    }

    Map<String, String> findUser(String userName) throws SQLException {
        return queryOne("SELECT * FROM oauth_user WHERE userName = ?", userName);
    }

    boolean verifyPassword(String userName, String password) throws SQLException {
        Map<String, String> user = findUser(userName);
        return user != null && user.get("password") != null && user.get("password").equals(password);
    }

    void saveAuthCode(String code, String clientId, String userName, String redirectUri, String scope) throws SQLException {
        execute("INSERT INTO oauth_auth_code (code, client_id, username, redirect_uri, scope, expires_at, used) VALUES (?,?,?,?,?,?,0)",
                code, clientId, userName, redirectUri, scope, now() + AUTH_CODE_EXPIRE);
    }

    /**
     * @return 未使用且未过期的授权码，不存在时返回 null
     */
    Map<String, String> findUsableAuthCode(String code, String clientId) throws SQLException {
        Map<String, String> authCode = queryOne("SELECT * FROM oauth_auth_code WHERE code = ? AND client_id = ?", code, clientId);
        if (authCode == null || "1".equals(authCode.get("used")) || expired(authCode.get("expires_at"))) {
            return null;
        }
        return authCode;
    }

    void consumeAuthCode(String id) throws SQLException {
        execute("UPDATE oauth_auth_code SET used = 1 WHERE id = ?", id);
    }

    /**
     * 签发并持久化令牌。
     *
     * @return 可直接序列化的令牌响应内容
     */
    Map<String, Object> issueToken(String clientId, String userName, String scope) throws SQLException {
        String grantedScope = Security.isEmpty(scope) ? DEFAULT_SCOPE : scope;
        String accessToken = Security.randomToken(32);
        String refreshToken = Security.randomToken(32);
        long now = now();
        execute("INSERT INTO oauth_token (access_token, refresh_token, client_id, userName, token_type, scope, expires_at, refresh_expires_at) VALUES (?,?,?,?,?,?,?,?)",
                accessToken, refreshToken, clientId, userName, TOKEN_TYPE, grantedScope,
                now + ACCESS_TOKEN_EXPIRE, now + REFRESH_TOKEN_EXPIRE);

        Map<String, Object> token = new LinkedHashMap<>();
        token.put("access_token", accessToken);
        token.put("token_type", TOKEN_TYPE);
        token.put("expires_in", ACCESS_TOKEN_EXPIRE);
        token.put("refresh_token", refreshToken);
        token.put("scope", grantedScope);
        return token;
    }

    /**
     * @return 未过期的访问令牌，不存在或已过期时返回 null
     */
    Map<String, String> findValidAccessToken(String accessToken) throws SQLException {
        Map<String, String> token = queryOne("SELECT * FROM oauth_token WHERE access_token = ?", accessToken);
        return expired(token == null ? null : token.get("expires_at")) ? null : token;
    }

    /**
     * @return 未过期的刷新令牌，不存在或已过期时返回 null
     */
    Map<String, String> findValidRefreshToken(String refreshToken, String clientId) throws SQLException {
        Map<String, String> token = queryOne("SELECT * FROM oauth_token WHERE refresh_token = ? AND client_id = ?", refreshToken, clientId);
        return expired(token == null ? null : token.get("refresh_expires_at")) ? null : token;
    }

    /**
     * @return 访问令牌或刷新令牌匹配的记录，不存在时返回 null
     */
    Map<String, String> findToken(String token) throws SQLException {
        return queryOne("SELECT * FROM oauth_token WHERE access_token = ? OR refresh_token = ?", token, token);
    }

    void deleteToken(String id) throws SQLException {
        execute("DELETE FROM oauth_token WHERE id = ?", id);
    }

    // ==================== 控制台管理用 ====================

    List<String[]> page(String table, String searchColumn, int pageNum, int pageSize, String keyword, String[] listFields) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(table);
        String like = like(keyword);
        if (like != null) {
            sql.append(" WHERE ").append(searchColumn).append(" LIKE ?");
        }
        sql.append(" LIMIT ? OFFSET ?");

        List<String[]> result = new ArrayList<>();
        for (Map<String, String> row : select(sql.toString(), params(like, pageSize, (pageNum - 1) * pageSize))) {
            String[] data = new String[listFields.length];
            for (int i = 0; i < listFields.length; i++) {
                data[i] = row.getOrDefault(listFields[i].toLowerCase(Locale.ROOT), "");
            }
            result.add(data);
        }
        return result;
    }

    int count(String table, String searchColumn, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total FROM ").append(table);
        String like = like(keyword);
        if (like != null) {
            sql.append(" WHERE ").append(searchColumn).append(" LIKE ?");
        }
        Map<String, String> row = queryOne(sql.toString(), like == null ? new Object[0] : new Object[]{like});
        return row == null ? 0 : Integer.parseInt(row.get("total"));
    }

    Map<String, String> show(String table, String id) throws SQLException {
        return queryOne("SELECT * FROM " + table + " WHERE id = ?", id);
    }

    void insert(String table, Map<String, String> data) throws SQLException {
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (columns.length() > 0) {
                columns.append(',');
                placeholders.append(',');
            }
            columns.append(entry.getKey());
            placeholders.append('?');
            values.add(entry.getValue());
        }
        execute("INSERT INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")", values.toArray());
    }

    void update(String table, String id, Map<String, String> data) throws SQLException {
        StringBuilder assignments = new StringBuilder();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (assignments.length() > 0) assignments.append(',');
            assignments.append(entry.getKey()).append("=?");
            values.add(entry.getValue());
        }
        values.add(id);
        execute("UPDATE " + table + " SET " + assignments + " WHERE id = ?", values.toArray());
    }

    void delete(String table, String id) throws SQLException {
        execute("DELETE FROM " + table + " WHERE id = ?", id);
    }

    static Map<String, String> row(String... keyValues) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put(keyValues[i], keyValues[i + 1]);
        }
        return row;
    }

    private static Object[] params(String like, int pageSize, int offset) {
        return like == null ? new Object[]{pageSize, offset} : new Object[]{like, pageSize, offset};
    }

    private static String like(String keyword) {
        return Security.isEmpty(keyword) ? null : "%" + keyword + "%";
    }

    private static boolean expired(String expiresAt) {
        if (expiresAt == null) return true;
        try {
            return Long.parseLong(expiresAt) < now();
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static long now() {
        return System.currentTimeMillis() / 1000;
    }

    private Map<String, String> queryOne(String sql, Object... params) throws SQLException {
        List<Map<String, String>> rows = select(sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Map<String, String>> select(String sql, Object... params) throws SQLException {
        try (Connection connection = jdbcPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Map<String, String>> rows = new ArrayList<>();
                ResultSetMetaData metaData = resultSet.getMetaData();
                while (resultSet.next()) {
                    Map<String, String> row = new HashMap<>();
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        row.put(metaData.getColumnLabel(i).toLowerCase(Locale.ROOT), resultSet.getString(i));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    private void execute(String sql, Object... params) throws SQLException {
        try (Connection connection = jdbcPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            statement.executeUpdate();
        }
    }

    private static void bind(PreparedStatement statement, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }
}
