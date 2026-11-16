package qingzhou.app.oauth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qingzhou.api.AppContext;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.MessageDigest;
import qingzhou.jdbc.JdbcPool;

/**
 * JDBC 存储：通过平台 JdbcPool + 通用 DataSource 连接任意标准 JDBC 数据库。
 * SQL 与建表脚本独立在 resources/sql/schema.sql，避免把方言 DDL 散落在 Java 代码中。
 */
final class JdbcStore extends Store {
    private static final Pattern CREATE_TABLE =
            Pattern.compile("(?is)^\\s*CREATE\\s+TABLE\\s+([a-zA-Z0-9_]+)");
    private static final String SCHEMA_RESOURCE = "/sql/schema.sql";

    private final JdbcPool jdbcPool;
    private final DriverManagerDataSource dataSource;
    private final SqlDialect dialect;

    private JdbcStore(MessageDigest messageDigest, Crypto crypto, Map<String, String> config,
                      JdbcPool jdbcPool, DriverManagerDataSource dataSource) {
        super(messageDigest, crypto, config);
        this.jdbcPool = jdbcPool;
        this.dataSource = dataSource;
        this.dialect = SqlDialect.fromUrl(dataSource.getUrl());
    }

    static JdbcStore create(AppContext appContext, MessageDigest messageDigest, Crypto crypto,
                            Map<String, String> config) throws Exception {
        String name = config.getOrDefault("jdbc_name", "h2").trim();
        JdbcPool jdbcPool = appContext.getService(JdbcPool.class, name);
        if (jdbcPool == null) {
            throw new IllegalStateException("storage=jdbc 需要 qingzhou-jdbc~" + name
                    + " 连接池，请检查实例配置");
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                config.get("jdbc_url"), config.get("jdbc_driver"),
                config.get("jdbc_username"), config.get("jdbc_password"));
        jdbcPool.init(dataSource);
        return new JdbcStore(messageDigest, crypto, config, jdbcPool, dataSource);
    }

    @Override
    void init() throws Exception {
        List<String> statements = readSchema();
        try (Connection connection = jdbcPool.getConnection()) {
            for (String sql : statements) {
                String table = tableName(sql);
                if (table != null && tableExists(connection, table)) continue;
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                } catch (SQLException e) {
                    // 多实例并发或元数据未暴露时，建表冲突按已存在处理；其他错误继续抛出
                    if (!alreadyExists(e)) throw e;
                }
            }
        }
    }

    @Override
    void cleanup() throws SQLException {
        long now = now();
        execute("DELETE FROM " + AUTH_CODE_TABLE + " WHERE expires_at < ? OR (used = ? AND create_time < ?)",
                now, 1, now - 3600);
        execute("DELETE FROM " + TOKEN_TABLE + " WHERE expires_at < ? AND refresh_expires_at < ?",
                now, now);
    }

    @Override
    String storageMode() {
        return "jdbc";
    }

    @Override
    Map<String, String> findClient(String clientId) throws SQLException {
        return queryOne("SELECT * FROM " + CLIENT_TABLE + " WHERE client_id = ?", clientId);
    }

    @Override
    Map<String, String> findUser(String userName) throws SQLException {
        return queryOne("SELECT * FROM " + USER_TABLE + " WHERE username = ?", userName);
    }

    @Override
    void saveAuthCode(Map<String, String> authCode) throws SQLException {
        insertRow(AUTH_CODE_TABLE, authCode);
    }

    @Override
    Map<String, String> findUsableAuthCode(String code, String clientId) throws SQLException {
        Map<String, String> authCode = queryOne("SELECT * FROM " + AUTH_CODE_TABLE
                + " WHERE code = ? AND client_id = ?", code, clientId);
        if (authCode == null || "1".equals(authCode.get("used")) || expired(authCode, "expires_at")) return null;
        return authCode;
    }

    @Override
    boolean consumeAuthCode(String id) throws SQLException {
        return execute("UPDATE " + AUTH_CODE_TABLE + " SET used = 1 WHERE id = ? AND used = 0", id) == 1;
    }

    @Override
    void saveToken(Map<String, String> token) throws SQLException {
        insertRow(TOKEN_TABLE, token);
    }

    @Override
    Map<String, String> findValidAccessToken(String accessToken) throws SQLException {
        Map<String, String> token = queryOne("SELECT * FROM " + TOKEN_TABLE
                + " WHERE access_token = ?", accessToken);
        return expired(token, "expires_at") ? null : token;
    }

    @Override
    Map<String, String> findValidRefreshToken(String refreshToken, String clientId) throws SQLException {
        Map<String, String> token = queryOne("SELECT * FROM " + TOKEN_TABLE
                + " WHERE refresh_token = ? AND client_id = ?", refreshToken, clientId);
        return expired(token, "refresh_expires_at") ? null : token;
    }

    @Override
    Map<String, String> findToken(String token) throws SQLException {
        return queryOne("SELECT * FROM " + TOKEN_TABLE
                + " WHERE access_token = ? OR refresh_token = ?", token, token);
    }

    @Override
    boolean deleteToken(String id) throws SQLException {
        return execute("DELETE FROM " + TOKEN_TABLE + " WHERE id = ?", id) == 1;
    }

    @Override
    List<String[]> page(String table, String searchColumn, int pageNum, int pageSize,
                        String keyword, String[] listFields) throws SQLException {
        validate(table, searchColumn);
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(table);
        boolean filtered = !Security.isEmpty(keyword);
        if (filtered) sql.append(" WHERE LOWER(").append(searchColumn).append(") LIKE LOWER(?)");
        sql.append(" ORDER BY id");

        int size = Math.max(1, Math.min(pageSize, 100));
        int offset = Math.max(0, (Math.max(pageNum, 1) - 1) * size);
        List<Object> params = new ArrayList<>();
        if (filtered) params.add("%" + keyword + "%");
        if (dialect == SqlDialect.SQL_SERVER || dialect == SqlDialect.ORACLE) {
            sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
            params.add(offset);
            params.add(size);
        } else {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(size);
            params.add(offset);
        }

        List<String[]> result = new ArrayList<>();
        for (Map<String, String> row : select(sql.toString(), params.toArray())) {
            String[] data = new String[listFields.length];
            for (int i = 0; i < listFields.length; i++) {
                data[i] = row.getOrDefault(listFields[i].toLowerCase(Locale.ROOT), "");
            }
            result.add(data);
        }
        return result;
    }

    @Override
    int count(String table, String searchColumn, String keyword) throws SQLException {
        validate(table, searchColumn);
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total FROM ").append(table);
        boolean filtered = !Security.isEmpty(keyword);
        if (filtered) sql.append(" WHERE LOWER(").append(searchColumn).append(") LIKE LOWER(?)");
        Map<String, String> row = queryOne(sql.toString(), filtered ? new Object[]{"%" + keyword + "%"} : new Object[0]);
        return row == null ? 0 : Integer.parseInt(row.get("total"));
    }

    @Override
    Map<String, String> show(String table, String id) throws SQLException {
        Store.columns(table);
        return queryOne("SELECT * FROM " + table + " WHERE id = ?", id);
    }

    @Override
    void insertRow(String table, Map<String, String> row) throws SQLException {
        Store.columns(table);
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, String> entry : row.entrySet()) {
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

    @Override
    void updateRow(String table, String id, Map<String, String> row) throws SQLException {
        Store.columns(table);
        StringBuilder assignments = new StringBuilder();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, String> entry : row.entrySet()) {
            if (assignments.length() > 0) assignments.append(',');
            assignments.append(entry.getKey()).append("=?");
            values.add(entry.getValue());
        }
        values.add(id);
        execute("UPDATE " + table + " SET " + assignments + " WHERE id = ?", values.toArray());
    }

    @Override
    void deleteRow(String table, String id) throws SQLException {
        Store.columns(table);
        execute("DELETE FROM " + table + " WHERE id = ?", id);
    }

    @Override
    void deleteBy(String table, String column, String value) throws SQLException {
        validate(table, column);
        execute("DELETE FROM " + table + " WHERE " + column + " = ?", value);
    }

    private static void validate(String table, String column) {
        Set<String> columns = Store.columns(table);
        if (!columns.contains(column.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("非法字段: " + column);
        }
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

    private int execute(String sql, Object... params) throws SQLException {
        try (Connection connection = jdbcPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        }
    }

    private static void bind(PreparedStatement statement, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) statement.setObject(i + 1, params[i]);
    }

    private static List<String> readSchema() throws IOException {
        try (InputStream input = JdbcStore.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (input == null) throw new IOException("缺少 SQL 初始化脚本: " + SCHEMA_RESOURCE);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            String text = new String(output.toByteArray(), StandardCharsets.UTF_8);
            List<String> statements = new ArrayList<>();
            for (String part : text.split(";")) {
                String sql = stripComments(part).trim();
                if (!sql.isEmpty()) statements.add(sql);
            }
            return statements;
        }
    }

    private static String stripComments(String sql) {
        StringBuilder result = new StringBuilder();
        for (String line : sql.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("--")) result.append(line).append('\n');
        }
        return result.toString();
    }

    private static String tableName(String sql) {
        Matcher matcher = CREATE_TABLE.matcher(sql);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(), null, "%", new String[]{"TABLE", "BASE TABLE"})) {
            while (tables.next()) {
                if (table.equalsIgnoreCase(tables.getString("TABLE_NAME"))) return true;
            }
        }
        return false;
    }

    private static boolean alreadyExists(SQLException e) {
        String state = e.getSQLState();
        if (state != null && ("42S01".equals(state) || "42P07".equals(state) || "42710".equals(state)
                 || "X".equals(state) || "1050".equals(state) || "2714".equals(state))) return true;
        String message = e.getMessage();
        if (message == null) return false;
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("already exists") || lower.contains("already exist")
                || lower.contains("already used");
    }

    private enum SqlDialect {
        STANDARD, SQL_SERVER, ORACLE;

        static SqlDialect fromUrl(String url) {
            String lower = url == null ? "" : url.toLowerCase(Locale.ROOT);
            if (lower.startsWith("jdbc:sqlserver:")) return SQL_SERVER;
            if (lower.startsWith("jdbc:oracle:")) return ORACLE;
            return STANDARD;
        }
    }
}
