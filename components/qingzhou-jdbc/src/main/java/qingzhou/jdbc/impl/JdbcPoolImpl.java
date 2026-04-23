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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import qingzhou.jdbc.JdbcPool;

@Component(configurationPid = "qingzhou-jdbc", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class JdbcPoolImpl implements JdbcPool {
    private org.apache.tomcat.jdbc.pool.DataSource dataSource;
    private URLClassLoader urlClassLoader;

    @Activate
    public void open(Map<String, String> config) throws Exception {
        PoolProperties poolProperties = new PoolProperties();

        setConfig(poolProperties, config);

        if (config.get("dataSourceClassName") != null) {
            File lib = new File(System.getProperty("qingzhou.instance"), "lib");
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
            CommonDataSource dataSource = (CommonDataSource) dsClass.newInstance();
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
        dataSource.close(true);

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
                        try {
                            writeMethod.invoke(object, entry.getValue());
                        } catch (java.lang.IllegalArgumentException e1) {
                            try {
                                writeMethod.invoke(object, Integer.parseInt(entry.getValue()));
                            } catch (java.lang.IllegalArgumentException e2) {
                                try {
                                    writeMethod.invoke(object, Long.parseLong(entry.getValue()));
                                } catch (java.lang.IllegalArgumentException e3) {
                                    writeMethod.invoke(object, Boolean.parseBoolean(entry.getValue()));
                                }
                            }
                        }
                    }

                    break;
                }
            }
        }
    }
}
