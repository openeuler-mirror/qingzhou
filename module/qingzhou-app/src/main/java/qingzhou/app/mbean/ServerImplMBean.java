package qingzhou.app.mbean;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public interface ServerImplMBean {

    List<Map<String, String>> list(String appName, String modelName) throws Exception;

    Map<String, String> show(String appName, String modelName, String name) throws Exception;

    Map<String, String> monitor(String appName, String modelName, String name) throws Exception;
}
