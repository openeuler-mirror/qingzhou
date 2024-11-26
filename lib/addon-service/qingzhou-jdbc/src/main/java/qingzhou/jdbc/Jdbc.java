package qingzhou.jdbc;

import qingzhou.engine.Service;

@Service(name = "Jdbc DB Connection", description = "Provides JDBC database connection pool services and supports XA data sources.")
public interface Jdbc {
    DataSourceBuilder createDataSourceBuilder();
}
