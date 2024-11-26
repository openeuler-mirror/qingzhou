package qingzhou.jdbc;

import qingzhou.engine.Service;

@Service(name = "Jdbc Connection Pool", description = "Provides JDBC database connection pool services and supports XA connections.")
public interface Jdbc {
    ConnectionPoolBuilder createDataSourceBuilder();
}
