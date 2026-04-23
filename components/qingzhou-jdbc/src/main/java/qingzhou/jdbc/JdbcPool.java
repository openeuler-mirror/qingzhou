package qingzhou.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.XAConnection;

public interface JdbcPool {
    Connection getConnection() throws SQLException;

    XAConnection getXAConnection() throws SQLException;
}
