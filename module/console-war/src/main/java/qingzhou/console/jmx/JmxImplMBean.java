package qingzhou.console.jmx;

import java.util.Properties;

/**
 * 给客户端使用
 */
public interface JmxImplMBean {
    String callServerMethod(String modelName, String actionName, Properties args) throws Exception;
}
