package qingzhou.jdbc;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionPool {
    Connection getConnection() throws SQLException;

    XAConnection getXAConnection() throws SQLException;
}
