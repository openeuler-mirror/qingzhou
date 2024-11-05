package qingzhou.deployer;

import qingzhou.engine.ServiceInfo;

import javax.management.NotificationListener;
import javax.management.remote.JMXAuthenticator;
import java.util.Properties;

public interface JmxServiceAdapter extends ServiceInfo {
    @Override
    default boolean isAppShared() {
        return false;
    }

    void registerJMXAuthenticator(JMXAuthenticator authenticator);

    void registerJmxInvoker(JmxInvoker jmxInvoker);

    void registerNotificationListener(NotificationListener notificationListener);

    interface JmxInvoker {
        String invoke(String appName, String modelName, String actionName, Properties args);
    }
}
