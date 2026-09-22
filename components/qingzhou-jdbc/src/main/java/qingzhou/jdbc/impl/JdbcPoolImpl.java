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

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.osgi.service.component.annotations.*;
import qingzhou.crypto.Crypto;
import qingzhou.jdbc.JdbcPool;

@Component(configurationPid = "qingzhou-jdbc", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class JdbcPoolImpl implements JdbcPool {
    @Reference
    private Crypto crypto;

    private PoolProperties poolConfig;
    private org.apache.tomcat.jdbc.pool.DataSource dsPool;

    @Activate
    public void setConfig(Map<String, String> config) throws Exception {
        PoolProperties poolConfig = new PoolProperties();
        BeanInfo beanInfo = Introspector.getBeanInfo(poolConfig.getClass(), Object.class);
        for (Map.Entry<String, String> entry : config.entrySet()) {
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if (pd.getName().equals(entry.getKey())) {
                    Method writeMethod = pd.getWriteMethod();
                    if (writeMethod != null) {
                        Object value = convert(entry.getValue(), writeMethod.getParameterTypes()[0]);
                        writeMethod.invoke(poolConfig, value);
                    }
                    break;
                }
            }
        }

        this.poolConfig = poolConfig;
    }

    // 按写入方法的参数类型做一次显式转换，替代嵌套 try-catch 试探；数值非法时抛 NumberFormatException 暴露配置笔误
    private Object convert(String value, Class<?> type) {
        if (type == int.class || type == Integer.class) return Integer.valueOf(value);
        if (type == long.class || type == Long.class) return Long.valueOf(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.valueOf(value);
        return value;
    }

    @Override
    public void init(CommonDataSource dataSource) throws Exception {
        if (dataSource == null) throw new IllegalArgumentException("dataSource required");
        close(); // 重新初始化

        poolConfig.setDataSource(dataSource);
        if (poolConfig.getUrl() != null) {
            for (Method method : dataSource.getClass().getMethods()) { // 某些实现需要设置 url
                if (method.getName().equalsIgnoreCase("setUrl")
                        && method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0] == String.class) {
                    method.invoke(dataSource, poolConfig.getUrl());
                }
            }
        }
        String password = poolConfig.getPassword();
        if (password != null && !password.trim().isEmpty()) {
            String decrypt = crypto.getGlobalCipher().tryDecrypt(password.trim(), "qingzhou-jdbc.password");
            poolConfig.setPassword(decrypt);
        }

        // 默认参数
        poolConfig.setJmxEnabled(false);
        poolConfig.setMaxIdle(poolConfig.getMaxActive()); // 避免警告

        boolean supportXA = poolConfig.getDataSource() instanceof javax.sql.XADataSource;
        dsPool = supportXA ?
                new org.apache.tomcat.jdbc.pool.XADataSource(poolConfig) :
                new org.apache.tomcat.jdbc.pool.DataSource(poolConfig);
        //initialise the pool itself
        dsPool.createPool();
    }

    @Deactivate
    public void close() {
        if (dsPool != null) dsPool.close(true); // open 失败时 OSGi 仍会回调 deactivate，避免 NPE
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dsPool.getConnection();
    }

    @Override
    public XAConnection getXAConnection() throws SQLException {
        return dsPool.getXAConnection();
    }
}
