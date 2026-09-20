package qingzhou.jdbc.impl;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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

    private org.apache.tomcat.jdbc.pool.DataSource dataSource;
    private URLClassLoader urlClassLoader;

    @Activate
    public void open(Map<String, String> config) throws Exception {
        String passwordKey = "password";
        String password = config.get(passwordKey);
        if (password != null && !password.trim().isEmpty()) {
            String decrypt = crypto.getGlobalCipher().tryDecrypt(password.trim(), "qingzhou-jdbc.password");
            config.put(passwordKey, decrypt);
        }

        PoolProperties poolConfig = new PoolProperties();
        setConfig(poolConfig, config);
        createPool(poolConfig, config);
    }

    private void createPool(PoolProperties poolProperties, Map<String, String> config) throws Exception {
        if (config.get("dataSourceClassName") != null) {
            File lib = new File(config.get("qingzhou.instance"), "lib");
            if (lib.isDirectory()) {
                List<URL> urls = new ArrayList<>();
                urls.add(lib.toURI().toURL());
                File[] jarFiles = lib.listFiles(f -> f.getName().endsWith(".jar"));
                if (jarFiles != null) {
                    for (File jarFile : jarFiles) {
                        urls.add(jarFile.toURI().toURL());
                    }
                }
                urlClassLoader = new URLClassLoader(urls.toArray(new URL[0]));
            }

            ClassLoader loader = urlClassLoader != null ? urlClassLoader : this.getClass().getClassLoader();
            Class<?> dsClass = loader.loadClass(config.get("dataSourceClassName"));
            CommonDataSource dataSource = (CommonDataSource) dsClass.getDeclaredConstructor().newInstance();
            poolProperties.setDataSource(dataSource);

            if (config.get("url") != null) {
                for (Method method : dataSource.getClass().getMethods()) { // 某些实现需要设置 url
                    if (method.getName().equalsIgnoreCase("setUrl")
                            && method.getParameterTypes().length == 1
                            && method.getParameterTypes()[0] == String.class) {
                        method.invoke(dataSource, config.get("url"));
                    }
                }
            }
        }

        // 默认参数
        poolProperties.setJmxEnabled(false);
        poolProperties.setMaxIdle(poolProperties.getMaxActive()); // 避免警告

        boolean supportXA = poolProperties.getDataSource() instanceof javax.sql.XADataSource;
        dataSource = supportXA ?
                new org.apache.tomcat.jdbc.pool.XADataSource(poolProperties) :
                new org.apache.tomcat.jdbc.pool.DataSource(poolProperties);
        //initialise the pool itself
        dataSource.createPool();
    }

    @Deactivate
    public void close() {
        if (dataSource != null) dataSource.close(true); // open 失败时 OSGi 仍会回调 deactivate，避免 NPE

        if (urlClassLoader != null) {
            try {
                urlClassLoader.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public XAConnection getXAConnection() throws SQLException {
        return dataSource.getXAConnection();
    }

    private void setConfig(Object object, Map<String, String> config) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass(), Object.class);

        for (Map.Entry<String, String> entry : config.entrySet()) {
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if (pd.getName().equals(entry.getKey())) {
                    Method writeMethod = pd.getWriteMethod();
                    if (writeMethod != null) {
                        Object value = convert(entry.getValue(), writeMethod.getParameterTypes()[0]);
                        writeMethod.invoke(object, value);
                    }

                    break;
                }
            }
        }
    }

    // 按写入方法的参数类型做一次显式转换，替代嵌套 try-catch 试探；数值非法时抛 NumberFormatException 暴露配置笔误
    private Object convert(String value, Class<?> type) {
        if (type == int.class || type == Integer.class) return Integer.valueOf(value);
        if (type == long.class || type == Long.class) return Long.valueOf(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.valueOf(value);
        return value;
    }
}
