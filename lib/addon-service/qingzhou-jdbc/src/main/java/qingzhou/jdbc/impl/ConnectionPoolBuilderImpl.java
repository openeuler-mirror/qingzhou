package qingzhou.jdbc.impl;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import qingzhou.jdbc.ConnectionPool;
import qingzhou.jdbc.ConnectionPoolBuilder;

import javax.sql.CommonDataSource;
import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionPoolBuilderImpl implements ConnectionPoolBuilder {
    private final PoolProperties poolProperties = new PoolProperties();

    @Override
    public ConnectionPoolBuilder dataSource(CommonDataSource dataSource) {
        poolProperties.setDataSource(dataSource);
        return this;
    }

    @Override
    public ConnectionPoolBuilder username(String username) {
        poolProperties.setUsername(username);
        return this;
    }

    @Override
    public ConnectionPoolBuilder password(String password) {
        poolProperties.setPassword(password);
        return this;
    }

    @Override
    public ConnectionPoolBuilder maxPoolSize(int maxPoolSize) {
        poolProperties.setMaxActive(maxPoolSize);
        return this;
    }

    @Override
    public ConnectionPoolBuilder minPoolSize(int minPoolSize) {
        poolProperties.setMinIdle(minPoolSize);
        return this;
    }

    @Override
    public ConnectionPoolBuilder borrowTimeout(int seconds) {
        poolProperties.setMaxWait(seconds * 1000);
        return this;
    }

    @Override
    public ConnectionPoolBuilder maxAge(int seconds) {
        poolProperties.setMaxAge(seconds * 1000L);
        return this;
    }

    @Override
    public ConnectionPoolBuilder validationInterval(int seconds) {
        poolProperties.setValidationInterval(seconds * 1000L);
        return this;
    }

    @Override
    public ConnectionPoolBuilder validationTimeout(int seconds) {
        poolProperties.setValidationQueryTimeout(seconds);
        return this;
    }

    @Override
    public ConnectionPoolBuilder connectionTestQuery(String testQuery) {
        poolProperties.setValidationQuery(testQuery);
        return this;
    }

    @Override
    public ConnectionPool buildConnectionPool() throws SQLException {
        String error = validate();
        if (error != null) {
            throw new SQLException(error);
        }

        boolean XA = poolProperties.getDataSource() instanceof javax.sql.XADataSource;
        org.apache.tomcat.jdbc.pool.DataSource dataSource = XA ?
                new org.apache.tomcat.jdbc.pool.XADataSource(poolProperties) :
                new org.apache.tomcat.jdbc.pool.DataSource(poolProperties);
        //initialise the pool itself
        dataSource.createPool();
        // Return the configured DataSource instance
        return buildConnectionPool(dataSource);
    }

    private ConnectionPool buildConnectionPool(org.apache.tomcat.jdbc.pool.DataSource dataSource) {
        Runnable shutdownHook = dataSource::close;
        Controller.shutdownHookList.add(shutdownHook);

        return new ConnectionPool() {
            @Override
            public Connection getConnection() throws SQLException {
                return dataSource.getConnection();
            }

            @Override
            public XAConnection getXAConnection() throws SQLException {
                return dataSource.getXAConnection();
            }

            @Override
            public void close() {
                dataSource.close(true);

                Controller.shutdownHookList.remove(shutdownHook);
            }
        };
    }

    private String validate() {
        if (poolProperties.getDataSource() == null) return "The dataSource parameter is missing";
        return null;
    }
}
