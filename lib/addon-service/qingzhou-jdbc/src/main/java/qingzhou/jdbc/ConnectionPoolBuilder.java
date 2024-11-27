package qingzhou.jdbc;

import javax.sql.CommonDataSource;
import java.sql.SQLException;

public interface ConnectionPoolBuilder {
    /**
     * Injects a datasource that will be used to retrieve/create connections.
     * If the data source implements {@link javax.sql.XADataSource} the methods
     * {@link javax.sql.XADataSource#getXAConnection()} and {@link javax.sql.XADataSource#getXAConnection(String, String)}
     * will be invoked.
     *
     * @param dataSource the {@link javax.sql.DataSource} to be used for creating connections to be pooled.
     */
    ConnectionPoolBuilder dataSource(CommonDataSource dataSource);

    /**
     * 连接数据库的用户名，非必填
     * Sets the username used to establish the connection with
     * It will also be a property called 'user' in the database properties.
     *
     * @param username The user name
     */
    ConnectionPoolBuilder username(String username);

    /**
     * 连接数据库的密码，非必填
     * Sets the password to establish the connection with.
     * The password will be included as a database property with the name 'password'.
     *
     * @param password The password
     */
    ConnectionPoolBuilder password(String password);

    /**
     * 池中最大连接数量
     * The maximum number of active connections that can be allocated from this pool at the same time. The default value is 100
     *
     * @param maxPoolSize hard limit for number of managed connections by this pool
     */
    ConnectionPoolBuilder maxPoolSize(int maxPoolSize);

    /**
     * 池中最少连接数量
     * The minimum number of established connections that should be kept in the pool at all times.
     * The connection pool can shrink below this number if validation queries fail and connections get closed.
     *
     * @param minPoolSize the minimum number of idle or established connections
     */
    ConnectionPoolBuilder minPoolSize(int minPoolSize);

    /**
     * 从连接池获取连接的最大超时时间
     * The maximum number of seconds that the pool will wait (when there are no available connections and the
     * {@link #maxPoolSize} has been reached) for a connection to be returned
     * before throwing an exception. Default value is 30 (30 seconds)
     *
     * @param seconds the maximum number of seconds to wait.
     */
    ConnectionPoolBuilder borrowTimeout(int seconds);

    /**
     * 连接存活的最大时间；0 表示没有限制
     * Time in seconds to keep this connection before reconnecting.
     * When a connection is idle, returned to the pool or borrowed from the pool, the pool will
     * check to see if the ((now - time-when-connected) &gt; maxAge) has been reached, and if so,
     * it reconnects.
     * The default value is 0, which implies that connections will be left open and no
     * age checks will be done.
     * This is a useful setting for database sessions that leak memory as it ensures that the session
     * will have a finite life span.
     *
     * @param seconds the time in seconds a connection will be open for when used
     */
    ConnectionPoolBuilder maxAge(int seconds);

    /**
     * 保持空闲连接可用的检测频率，0 表示不检测
     * avoid excess validation, only run validation at most at this frequency - time in seconds.
     * If a connection is due for validation, but has been validated previously
     * within this interval, it will not be validated again.
     * The default value is 3 (3 seconds).
     *
     * @param seconds the validation interval in seconds
     */
    ConnectionPoolBuilder validationInterval(int seconds);

    /**
     * 连接检测的超时时间
     * The timeout in seconds before a connection validation queries fail.
     * A value less than or equal to zero will disable this feature.  Defaults to -1.
     *
     * @param seconds The timeout value
     */
    ConnectionPoolBuilder validationTimeout(int seconds);

    /**
     * 连接检测的查询语句；如果驱动支持 JDBC 4，则建议不要设置此参数
     * The SQL query that will be used to validate connections from this
     * pool before returning them to the caller or pool.
     * If specified, this query does not have to return any data,
     * it just can't throw an SQLException.
     * The default value is null.
     * Example values are SELECT 1(mysql),
     * select 1 from dual(oracle),
     * SELECT 1(MS Sql Server)
     *
     * @param testQuery the query used for validation or null if no validation is performed
     */
    ConnectionPoolBuilder connectionTestQuery(String testQuery);

    /**
     * 在设置完毕所有的参数后，最后调用这个方法，获得连接池对象。
     * 注：该方法每调用一次，都会新建立一个连接池对象，请谨慎使用，
     * 对于不再使用的池对象，则应该及时调用其 close() 方法，以释放系统资源！
     *
     * @return 配置好的连接池
     */
    ConnectionPool buildConnectionPool() throws SQLException;
}
