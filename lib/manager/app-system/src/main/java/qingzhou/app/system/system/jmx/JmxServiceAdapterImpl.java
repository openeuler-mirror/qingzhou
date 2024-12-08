package qingzhou.app.system.system.jmx;

import javax.management.NotificationListener;
import javax.management.remote.JMXAuthenticator;

import qingzhou.core.console.JmxServiceAdapter;

class JmxServiceAdapterImpl implements JmxServiceAdapter {
    private static final JmxServiceAdapterImpl INSTANCE = new JmxServiceAdapterImpl();

    static JmxServiceAdapterImpl getInstance() {
        return INSTANCE;
    }

    JMXAuthenticator authenticator;
    JmxInvoker jmxInvoker;
    NotificationListener notificationListener;

    @Override
    public void registerJMXAuthenticator(JMXAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public void registerJmxInvoker(JmxInvoker jmxInvoker) {
        this.jmxInvoker = jmxInvoker;
    }

    @Override
    public void registerNotificationListener(NotificationListener notificationListener) {
        this.notificationListener = notificationListener;
    }
}
