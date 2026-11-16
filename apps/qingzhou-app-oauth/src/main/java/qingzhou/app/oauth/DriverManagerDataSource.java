package qingzhou.app.oauth;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * 通用 JDBC DataSource：不绑定具体数据库，按 URL 或配置的驱动类直接实例化驱动。
 *
 * 平台 JdbcPool 会通过 setUrl 注入 qingzhou-jdbc~&lt;name&gt;.url，并把用户名/口令作为
 * getConnection(user, password) 参数传入；应用无需依赖某个厂商的 DataSource 实现。
 */
public final class DriverManagerDataSource implements DataSource {
    private String url;
    private final String driverClass;
    private final String username;
    private final String password;
    private volatile Driver driver;

    DriverManagerDataSource(String url, String driverClass, String username, String password) {
        this.url = url;
        this.driverClass = driverClass;
        this.username = username;
        this.password = password;
    }

    public void setUrl(String url) {
        this.url = url;
        this.driver = null;
    }

    String getUrl() {
        return url;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connect(username, password);
    }

    @Override
    public Connection getConnection(String user, String pass) throws SQLException {
        return connect(user == null ? username : user, pass == null ? password : pass);
    }

    private Connection connect(String user, String pass) throws SQLException {
        if (url == null || url.trim().isEmpty()) {
            throw new SQLException("JDBC URL 未配置，请设置 qingzhou-jdbc~<name>.url");
        }
        Properties properties = new Properties();
        if (user != null) properties.setProperty("user", user);
        if (pass != null) properties.setProperty("password", pass);
        Connection connection = driver().connect(url, properties);
        if (connection == null) throw new SQLException("JDBC 驱动不接受配置的 URL");
        return connection;
    }

    private Driver driver() throws SQLException {
        Driver current = driver;
        if (current != null) return current;
        synchronized (this) {
            if (driver != null) return driver;
            String className = driverClass;
            if (className == null || className.trim().isEmpty()) className = inferDriver(url);
            if (className == null || className.trim().isEmpty()) {
                throw new SQLException("无法推断 JDBC 驱动，请设置 app~qingzhou-app-oauth.jdbc_driver");
            }
            try {
                Class<?> type = Class.forName(className.trim(), true, DriverManagerDataSource.class.getClassLoader());
                driver = (Driver) type.getDeclaredConstructor().newInstance();
                return driver;
            } catch (Exception e) {
                throw new SQLException("加载 JDBC 驱动失败: " + className, e);
            }
        }
    }

    static String inferDriver(String url) {
        if (url == null) return null;
        String lower = url.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("jdbc:h2:")) return "org.h2.Driver";
        if (lower.startsWith("jdbc:postgresql:")) return "org.postgresql.Driver";
        if (lower.startsWith("jdbc:mysql:")) return "com.mysql.cj.jdbc.Driver";
        if (lower.startsWith("jdbc:mariadb:")) return "org.mariadb.jdbc.Driver";
        if (lower.startsWith("jdbc:sqlite:")) return "org.sqlite.JDBC";
        if (lower.startsWith("jdbc:sqlserver:")) return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        return null;
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
    }

    @Override
    public void setLoginTimeout(int seconds) {
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getLogger("qingzhou.oauth.jdbc");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("not a wrapper for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }
}
