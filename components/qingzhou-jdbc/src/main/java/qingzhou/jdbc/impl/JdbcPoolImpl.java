package qingzhou.jdbc.impl;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.CommonDataSource;
import javax.sql.XAConnection;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.XADataSource;
import org.osgi.service.component.annotations.*;
import qingzhou.crypto.Crypto;
import qingzhou.jdbc.JdbcPool;

@Component(configurationPid = "qingzhou-jdbc", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class JdbcPoolImpl implements JdbcPool {
    @Reference
    private Crypto crypto;

    private PoolProperties poolConfig;
    private DataSource dsPool;

    @Activate
    public void activate(Map<String, String> config) throws Exception {
        PoolProperties props = new PoolProperties();
        BeanInfo beanInfo = Introspector.getBeanInfo(props.getClass(), Object.class);
        for (Map.Entry<String, String> entry : config.entrySet()) {
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if (pd.getName().equals(entry.getKey())) {
                    Method writeMethod = pd.getWriteMethod();
                    if (writeMethod != null) {
                        writeMethod.invoke(props, convert(entry.getValue(), writeMethod.getParameterTypes()[0]));
                    }
                    break;
                }
            }
        }
        this.poolConfig = props;
    }

    private Object convert(String value, Class<?> type) {
        if (type == int.class || type == Integer.class) return Integer.valueOf(value);
        if (type == long.class || type == Long.class) return Long.valueOf(value);
        if (type == boolean.class || type == Boolean.class) {
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) return Boolean.valueOf(value);
            throw new IllegalArgumentException("invalid boolean: " + value);
        }
        return value;
    }

    @Override
    public void init(CommonDataSource dataSource) throws Exception {
        if (dataSource == null) throw new IllegalArgumentException("dataSource required");
        close();

        poolConfig.setDataSource(dataSource);
        if (poolConfig.getUrl() != null) {
            for (Method method : dataSource.getClass().getMethods()) {
                if (method.getName().equalsIgnoreCase("setUrl")
                        && method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0] == String.class) {
                    method.invoke(dataSource, poolConfig.getUrl());
                    break;
                }
            }
        }
        String password = poolConfig.getPassword();
        if (password != null && !password.trim().isEmpty()) {
            poolConfig.setPassword(crypto.getGlobalCipher().tryDecrypt(password.trim(), "qingzhou-jdbc.password"));
        }

        poolConfig.setJmxEnabled(false);
        poolConfig.setMaxIdle(poolConfig.getMaxActive());
        dsPool = poolConfig.getDataSource() instanceof javax.sql.XADataSource
                ? new XADataSource(poolConfig) : new DataSource(poolConfig);
        dsPool.createPool();
    }

    @Deactivate
    public void close() {
        if (dsPool != null) {
            dsPool.close(true); // activate 失败时 OSGi 仍回调 deactivate，避免 NPE
            dsPool = null;
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (dsPool == null) throw new IllegalStateException("pool not initialized");
        return dsPool.getConnection();
    }

    @Override
    public XAConnection getXAConnection() throws SQLException {
        if (dsPool == null) throw new IllegalStateException("pool not initialized");
        return dsPool.getXAConnection();
    }
}
