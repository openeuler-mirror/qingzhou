package qingzhou.jdbc;

import javax.sql.CommonDataSource;

public interface ConnectionPoolBuilder {
    // 驱动里面数据源的类名称
    ConnectionPoolBuilder dataSource(CommonDataSource dataSource);

    // 连接数据库的用户名，非必填
    ConnectionPoolBuilder username(String username);

    // 连接数据库的密码，非必填
    ConnectionPoolBuilder password(String password);

    // 池中最大连接数量
    ConnectionPoolBuilder maxPoolSize(int maxPoolSize);

    // 池中最少连接数量
    ConnectionPoolBuilder minPoolSize(int minPoolSize);

    // 从连接池获取连接的最大超时时间
    ConnectionPoolBuilder pollTimeout(int seconds);

    // 连接存活的最大时间；0 表示没有限制
    ConnectionPoolBuilder maxLifetime(int seconds);

    // 保持空闲连接可用的检测频率，0 表示不检测
    ConnectionPoolBuilder keepaliveTime(int seconds);

    // 连接检测的超时时间
    ConnectionPoolBuilder validationTimeout(int seconds);

    // 连接检测的查询语句；如果驱动支持 JDBC 4，则建议不要设置此参数
    ConnectionPoolBuilder connectionTestQuery(String testQuery);

    ConnectionPool buildConnectionPool();
}
