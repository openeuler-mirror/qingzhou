package qingzhou.console.jmx;

import java.util.Properties;

/**
 * 给客户端使用
 */
public interface ConsoleJmxMBean {
    String invoke(String appName, String modelName, String actionName, Properties args) throws Exception;
}
