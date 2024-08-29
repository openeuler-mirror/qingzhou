package qingzhou.app.master.jmx;

import java.util.Properties;

class ConsoleJmx implements ConsoleJmxMBean {

    @Override
    public String invoke(String appName, String modelName, String actionName, Properties args) {
        if (JmxServiceAdapterImpl.getInstance().jmxInvoker != null) {
            return JmxServiceAdapterImpl.getInstance().jmxInvoker.invoke(appName, modelName, actionName, args);
        }
        return null;
    }
}
