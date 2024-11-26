package qingzhou.jdbc;

public interface DataSourceBuilder {
    // 驱动里面数据源的类名称
    DataSourceBuilder dataSourceClassName(String dataSourceClassName);

    // 连接 url；该参数与 dataSourceClassName 设置一个即可
    DataSourceBuilder jdbcUrl(String jdbcUrl);

    // 但设置了 jdbcUrl 时，需要设置该参数
    DataSourceBuilder jdbcDriver(String jdbcDriver);

    // 连接数据库的用户名
    DataSourceBuilder username(String username);

    // 连接数据库的密码
    DataSourceBuilder password(String password);

    // 连接返回连接池时，是否自动提交事务
    DataSourceBuilder autoCommit(boolean autoCommit);

    // 池中最大连接数量
    DataSourceBuilder maxPoolSize(int maxSize);

    // 池中最少连接数量
    DataSourceBuilder minPoolSize(int minSize);

    // 从连接池获取连接的最大超时时间
    DataSourceBuilder pollTimeout(int seconds);

    // 连接存活的最大时间；0 表示没有限制
    DataSourceBuilder maxLifetime(int seconds);

    // 保持空闲连接可用的检测频率，0 表示不检测
    DataSourceBuilder keepaliveTime(int seconds);

    // 连接检测的超时时间
    DataSourceBuilder validationTimeout(int seconds);

    // 连接检测的查询语句；如果驱动支持 JDBC 4，则建议不要设置此参数
    DataSourceBuilder connectionTestQuery(String sql);

    // 连接可以被借出多久；超过该时间将打印连接可能泄露的日志
    DataSourceBuilder leakDetectionThreshold(int seconds);

    <T> T buildDataSource();
}
