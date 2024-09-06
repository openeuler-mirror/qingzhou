package qingzhou.app.system.jmx;

import qingzhou.deployer.JmxServiceAdapter;

import javax.management.NotificationListener;
import javax.management.remote.JMXAuthenticator;

class JmxServiceAdapterImpl implements JmxServiceAdapter {
    private static final JmxServiceAdapterImpl instance = new JmxServiceAdapterImpl();

    static JmxServiceAdapterImpl getInstance() {
        return instance;
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
