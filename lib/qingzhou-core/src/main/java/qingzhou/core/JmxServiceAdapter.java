package qingzhou.core;

import qingzhou.engine.Service;

import javax.management.NotificationListener;
import javax.management.remote.JMXAuthenticator;
import java.util.Properties;

@Service(shareable = false)
public interface JmxServiceAdapter {
    void registerJMXAuthenticator(JMXAuthenticator authenticator);

    void registerJmxInvoker(JmxInvoker jmxInvoker);

    void registerNotificationListener(NotificationListener notificationListener);

    interface JmxInvoker {
        String invoke(String appName, String modelName, String actionName, Properties args);
    }
}
