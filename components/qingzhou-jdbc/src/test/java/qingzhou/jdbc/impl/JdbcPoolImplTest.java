package qingzhou.jdbc.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

public class JdbcPoolImplTest {

    @Test
    public void openedPool_getConnection_returnUsableConnection() throws Exception {
        JdbcPoolImpl pool = createPool(createConfig(1, 2, 1000));
        Connection connection = null;
        try {
            connection = pool.getConnection();
            Assert.assertNotNull(connection);
            Assert.assertFalse(connection.isClosed());
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                Assert.assertTrue(resultSet.next());
                Assert.assertEquals(resultSet.getInt(1), 1);
            }
        } finally {
            close(connection);
            closePool(pool);
        }
    }

    @Test
    public void openedPool_getConnection_returnMultipleConnections() throws Exception {
        JdbcPoolImpl pool = createPool(createConfig(2, 3, 1000));
        Connection first = null;
        Connection second = null;
        try {
            first = pool.getConnection();
            second = pool.getConnection();
            Assert.assertNotNull(first);
            Assert.assertNotNull(second);
            Assert.assertNotSame(first, second);
            Assert.assertFalse(first.isClosed());
            Assert.assertFalse(second.isClosed());
        } finally {
            close(first);
            close(second);
            closePool(pool);
        }
    }

    @Test(timeOut = 5000)
    public void maxActiveLimit_exceedPoolLimit_throwSQLException() throws Exception {
        JdbcPoolImpl pool = createPool(createConfig(1, 1, 200));
        Connection first = null;
        try {
            first = pool.getConnection();
            Assert.assertNotNull(first);
            try {
                pool.getConnection();
                Assert.fail();
            } catch (SQLException e) {
                Assert.assertFalse(e.getMessage() == null || e.getMessage().isEmpty());
            }
        } finally {
            close(first);
            closePool(pool);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void invalidBoolean_activate_throwException() throws Exception {
        Map<String, String> config = createConfig(1, 1, 1000);
        config.put("testOnBorrow", "ture");
        new JdbcPoolImpl().activate(config);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void uninitializedPool_getConnection_throwIllegalStateException() throws SQLException {
        new JdbcPoolImpl().getConnection();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullDataSource_init_throwException() throws Exception {
        JdbcPoolImpl pool = createPool(createConfig(1, 1, 1000));
        try {
            pool.init(null);
        } finally {
            closePool(pool);
        }
    }

    private JdbcPoolImpl createPool(Map<String, String> config) throws Exception {
        JdbcPoolImpl pool = new JdbcPoolImpl();
        pool.activate(config);
        pool.init(new org.h2.jdbcx.JdbcDataSource());
        return pool;
    }

    private Map<String, String> createConfig(int initialSize, int maxActive, int maxWait) {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("url", "jdbc:h2:mem:" + UUID.randomUUID().toString().replace("-", ""));
        config.put("username", "sa");
        config.put("password", "");
        config.put("initialSize", Integer.toString(initialSize));
        config.put("minIdle", Integer.toString(initialSize));
        config.put("maxActive", Integer.toString(maxActive));
        config.put("maxWait", Integer.toString(maxWait));
        config.put("testOnBorrow", "true");
        config.put("validationQuery", "SELECT 1");
        return config;
    }

    private void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private void closePool(JdbcPoolImpl pool) {
        if (pool != null) {
            try {
                pool.close();
            } catch (RuntimeException ignored) {
            }
        }
    }
}
