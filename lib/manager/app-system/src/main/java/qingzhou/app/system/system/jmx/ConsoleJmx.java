package qingzhou.app.system.system.jmx;

import java.util.Properties;

import qingzhou.engine.util.Utils;

class ConsoleJmx implements ConsoleJmxMBean {

    @Override
    public String invoke(String appName, String modelName, String actionName, Properties args) {
        if (JmxServiceAdapterImpl.getInstance().jmxInvoker != null) {
            return JmxServiceAdapterImpl.getInstance().jmxInvoker.invoke(appName, modelName, actionName, args);
        }
        return null;
    }

    @Override
    public String invoke(String appName, String modelName, String actionName, String args) {
        Properties props = new Properties();
        if (Utils.notBlank(args)) {
            String[] split = args.split("&");
            for (String s : split) {
                int i = s.indexOf('=');
                if (i != -1) {
                    String key = s.substring(0, i);
                    if (Utils.notBlank(key)) {
                        String value = s.substring(i + 1);
                        props.setProperty(key, value);
                    }
                }
            }
        }
        return invoke(appName, modelName, actionName, props);
    }
}
