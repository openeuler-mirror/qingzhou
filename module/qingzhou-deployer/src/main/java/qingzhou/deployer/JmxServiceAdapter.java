package qingzhou.deployer;

import java.util.Properties;
import javax.management.NotificationListener;
import javax.management.remote.JMXAuthenticator;

public interface JmxServiceAdapter {
    void registerJMXAuthenticator(JMXAuthenticator authenticator);

    void registerJmxInvoker(JmxInvoker jmxInvoker);

    void registerNotificationListener(NotificationListener notificationListener);

    interface JmxInvoker {
        String invoke(String appName, String modelName, String actionName, Properties args);
    }
}
