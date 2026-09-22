package qingzhou.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.CommonDataSource;
import javax.sql.XAConnection;

public interface JdbcPool {
    void init(CommonDataSource dataSource) throws Exception;

    Connection getConnection() throws SQLException;

    XAConnection getXAConnection() throws SQLException;
}
