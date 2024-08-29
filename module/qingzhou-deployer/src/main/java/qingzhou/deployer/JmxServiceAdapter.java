package qingzhou.deployer;

import javax.management.NotificationListener;
import javax.management.remote.JMXAuthenticator;
import java.util.Properties;

public interface JmxServiceAdapter {
    void registerJMXAuthenticator(JMXAuthenticator authenticator);

    void registerJmxInvoker(JmxInvoker jmxInvoker);

    void registerNotificationListener(NotificationListener notificationListener);

    interface JmxInvoker {
        String invoke(String appName, String modelName, String actionName, Properties args);
    }
}
