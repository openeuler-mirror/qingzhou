package qingzhou.jdbc.impl;

import qingzhou.jdbc.ConnectionPool;
import qingzhou.jdbc.ConnectionPoolBuilder;

import javax.sql.CommonDataSource;

public class ConnectionPoolBuilderImpl implements ConnectionPoolBuilder {
    @Override
    public ConnectionPoolBuilder dataSource(CommonDataSource dataSource) {
        return null;
    }

    @Override
    public ConnectionPoolBuilder username(String username) {
        return null;
    }

    @Override
    public ConnectionPoolBuilder password(String password) {
        return null;
    }

    @Override
    public ConnectionPoolBuilder maxPoolSize(int maxPoolSize) {
        return null;
    }

    @Override
    public ConnectionPoolBuilder minPoolSize(int minPoolSize) {
        return null;
    }

    @Override
    public ConnectionPoolBuilder pollTimeout(int seconds) {
        return null;
    }

    @Override
    public ConnectionPoolBuilder maxLifetime(int seconds) {
        return null;
    }

    @Override
    public ConnectionPoolBuilder keepaliveTime(int seconds) {
        return null;
    }

    @Override
    public ConnectionPoolBuilder validationTimeout(int seconds) {
        return null;
    }

    @Override
    public ConnectionPoolBuilder connectionTestQuery(String testQuery) {
        return null;
    }

    @Override
    public ConnectionPool buildConnectionPool() {
        return null;
    }
}
