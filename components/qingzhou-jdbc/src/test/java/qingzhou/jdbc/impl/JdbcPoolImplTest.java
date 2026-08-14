package qingzhou.jdbc.impl;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
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

    @Test
    public void validConfig_open_applyConnectionPoolConfiguration() throws Exception {
        Map<String, String> config = createConfig(2, 5, 3000);
        JdbcPoolImpl pool = createPool(config);
        try {
            DataSource dataSource = getDataSource(pool);
            PoolProperties properties = (PoolProperties) dataSource.getPoolProperties();
            Assert.assertEquals(properties.getDriverClassName(), "org.h2.Driver");
            Assert.assertTrue(properties.getUrl().startsWith("jdbc:h2:mem:"));
            Assert.assertEquals(properties.getUsername(), "sa");
            Assert.assertEquals(properties.getPassword(), "");
            Assert.assertEquals(properties.getInitialSize(), 2);
            Assert.assertEquals(properties.getMinIdle(), 2);
            Assert.assertEquals(properties.getMaxActive(), 5);
            Assert.assertEquals(properties.getMaxWait(), 3000);
            Assert.assertTrue(properties.isTestOnBorrow());
            Assert.assertEquals(properties.getValidationQuery(), "SELECT 1");
            Assert.assertFalse(properties.isJmxEnabled());
        } finally {
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

    @Test
    public void closedConnection_getConnection_reusePooledConnection() throws Exception {
        JdbcPoolImpl pool = createPool(createConfig(1, 1, 500));
        Connection first = null;
        Connection second = null;
        try {
            first = pool.getConnection();
            Assert.assertFalse(first.isClosed());
            first.close();
            Assert.assertTrue(first.isClosed());
            second = pool.getConnection();
            Assert.assertNotNull(second);
            Assert.assertFalse(second.isClosed());
            Assert.assertEquals(getDataSource(pool).getPool().getSize(), 1);
            second.close();
            second = null;
        } finally {
            close(second);
            closePool(pool);
        }
    }

    @Test
    public void createdPool_poolStatus_initialConnectionsReady() throws Exception {
        JdbcPoolImpl pool = createPool(createConfig(2, 5, 1000));
        try {
            ConnectionPool connectionPool = getDataSource(pool).getPool();
            Assert.assertFalse(connectionPool.isClosed());
            Assert.assertEquals(connectionPool.getActive(), 0);
            Assert.assertTrue(connectionPool.getSize() >= 2);
            Assert.assertTrue(connectionPool.getIdle() >= 1);
        } finally {
            closePool(pool);
        }
    }

    @Test
    public void borrowedConnection_poolStatus_activeCountIncreased() throws Exception {
        JdbcPoolImpl pool = createPool(createConfig(1, 2, 1000));
        Connection connection = null;
        try {
            connection = pool.getConnection();
            ConnectionPool connectionPool = getDataSource(pool).getPool();
            Assert.assertEquals(connectionPool.getActive(), 1);
            Assert.assertEquals(connectionPool.getBorrowedCount(), 1L);
            Assert.assertEquals(connectionPool.getIdle(), 0);
        } finally {
            close(connection);
            closePool(pool);
        }
    }

    @Test
    public void returnedConnection_poolStatus_connectionReturnedToPool() throws Exception {
        JdbcPoolImpl pool = createPool(createConfig(1, 2, 1000));
        Connection connection = null;
        try {
            connection = pool.getConnection();
            connection.close();
            connection = null;
            ConnectionPool connectionPool = getDataSource(pool).getPool();
            Assert.assertEquals(connectionPool.getActive(), 0);
            Assert.assertEquals(connectionPool.getIdle(), 1);
            Assert.assertEquals(connectionPool.getBorrowedCount(), 1L);
        } finally {
            close(connection);
            closePool(pool);
        }
    }

    @Test
    public void closedPool_poolStatus_poolIsClosed() throws Exception {
        JdbcPoolImpl pool = createPool(createConfig(1, 2, 1000));
        try {
            ConnectionPool connectionPool = getDataSource(pool).getPool();
            pool.close();
            pool = null;
            Assert.assertTrue(connectionPool.isClosed());
        } finally {
            closePool(pool);
        }
    }

    private JdbcPoolImpl createPool(Map<String, String> config) throws Exception {
        JdbcPoolImpl pool = new JdbcPoolImpl();
        pool.open(config);
        return pool;
    }

    private Map<String, String> createConfig(int initialSize, int maxActive, int maxWait) {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("driverClassName", "org.h2.Driver");
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

    private DataSource getDataSource(JdbcPoolImpl pool) throws Exception {
        Field dataSourceField = JdbcPoolImpl.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        return (DataSource) dataSourceField.get(pool);
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
