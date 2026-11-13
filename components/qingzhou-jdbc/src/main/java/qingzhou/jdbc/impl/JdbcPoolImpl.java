package qingzhou.jdbc.impl;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.sql.CommonDataSource;
import javax.sql.XAConnection;

import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.XADataSource;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.*;
import qingzhou.crypto.Crypto;
import qingzhou.jdbc.JdbcPool;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.MetricProvider;

@Component(configurationPid = "qingzhou-jdbc", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = MetricProvider.PROVIDER_ID + "=jdbc")
public class JdbcPoolImpl implements JdbcPool, MetricProvider {
    private static final String POOL_LABEL = "pool";

    @Reference
    private Crypto crypto;

    private PoolProperties poolConfig;
    private DataSource dsPool;
    private String poolName;

    @Activate
    public void activate(Map<String, String> config) throws Exception {
        poolName = poolNameOf(config.get(Constants.SERVICE_PID));
        PoolProperties props = new PoolProperties();
        BeanInfo beanInfo = Introspector.getBeanInfo(props.getClass(), Object.class);
        Map<String, PropertyDescriptor> descriptors = new java.util.HashMap<>();
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            descriptors.put(pd.getName(), pd);
        }
        for (Map.Entry<String, String> entry : config.entrySet()) {
            // 契约适配：SCR 与平台注入的 service.*、qingzhou.* 不是池配置项，不参与 PoolProperties 映射
            if (isFrameworkProperty(entry.getKey())) continue;
            PropertyDescriptor matched = descriptors.get(entry.getKey());
            // 未知配置项 fail-fast：拼写错误（如 maxActiv）静默失效会掩盖真实的池配置
            if (matched == null || matched.getWriteMethod() == null) {
                throw new IllegalArgumentException("unknown qingzhou-jdbc config item: " + entry.getKey());
            }
            matched.getWriteMethod().invoke(props, convert(entry.getKey(), entry.getValue(), matched.getWriteMethod().getParameterTypes()[0]));
        }
        this.poolConfig = props;
    }

    private Object convert(String key, String value, Class<?> type) {
        try {
            // dataSource 必须由 init() 注入真实对象；从字符串配置触发 Tomcat DataSource 的 JNDI lookup 是注入入口，fail-closed 拒绝
            if (type == Object.class) {
                throw new IllegalArgumentException("config item [" + key + "] cannot be set from a string value");
            }
            if (type == int.class || type == Integer.class) return Integer.valueOf(value);
            if (type == long.class || type == Long.class) return Long.valueOf(value);
            if (type == short.class || type == Short.class) return Short.valueOf(value);
            if (type == float.class || type == Float.class) return Float.valueOf(value);
            if (type == double.class || type == Double.class) return Double.valueOf(value);
            if (type == boolean.class || type == Boolean.class) {
                // 与全局 Configs 的严格大小写策略一致，仅接受小写的 true/false
                if ("true".equals(value)) return Boolean.TRUE;
                if ("false".equals(value)) return Boolean.FALSE;
                throw new IllegalArgumentException("invalid boolean: " + value);
            }
            return value;
        } catch (NumberFormatException e) {
            // 异常须携带配置项名，否则反射调用栈里无法定位是哪个配置项的值非法
            throw new IllegalArgumentException("invalid value of config item [" + key + "]: " + value, e);
        }
    }

    // 工厂配置 pid 形如 qingzhou-jdbc~<池名>：池名是唯一可得的实例身份，指标维度、日志定位都依赖它
    private static String poolNameOf(String pid) {
        if (pid == null) throw new IllegalStateException("missing config item: " + Constants.SERVICE_PID);
        int index = pid.indexOf('~');
        return index < 0 ? pid : pid.substring(index + 1);
    }

    private static boolean isFrameworkProperty(String key) {
        return key.startsWith("service.") || key.startsWith("qingzhou.");
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
        if (password != null && !password.isEmpty()) {
            poolConfig.setPassword(crypto.getGlobalCipher().tryDecrypt(password, "qingzhou-jdbc.password"));
        }

        poolConfig.setJmxEnabled(false);
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

    /**
     * 直接读 Tomcat 连接池快照：池已关闭 JMX（见 init），指标是唯一的池水位出口。
     */
    @Override
    public Set<Metric> collectMetrics() {
        DataSource pool = dsPool;
        // 采集与 deactivate 并发时池可能已置空或关闭，本次不产出样本；正常态下二者非空
        ConnectionPool stats = pool == null ? null : pool.getPool();
        if (stats == null) return Collections.emptySet();

        // 多池并存：指标族相同，靠 pool 维度区分实例；缺该维度会与其它池构成重复序列
        Map<String, String> labels = Collections.singletonMap(POOL_LABEL, poolName);
        Set<Metric> metrics = new HashSet<>();
        metrics.add(Metric.of("pool_active", stats.getActive(), Metric.Type.GAUGE, labels,
                "Connections currently borrowed from the pool"));
        metrics.add(Metric.of("pool_idle", stats.getIdle(), Metric.Type.GAUGE, labels,
                "Idle connections currently held by the pool"));
        metrics.add(Metric.of("pool_waiters", stats.getWaitCount(), Metric.Type.GAUGE, labels,
                "Threads waiting to borrow a connection"));
        metrics.add(Metric.of("pool_size", stats.getSize(), Metric.Type.GAUGE, labels,
                "Connections currently tracked by the pool"));
        return metrics;
    }
}
