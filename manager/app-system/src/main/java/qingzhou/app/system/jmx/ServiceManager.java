package qingzhou.app.system.jmx;

import qingzhou.config.Jmx;
import qingzhou.engine.util.Utils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class ServiceManager {
    private static final ServiceManager instance = new ServiceManager();
    private JMXConnectorServer server;
    private ObjectName objectName;
    private MBeanServer mBeanServer;
    private Registry registry;

    static {
        if (RMISocketFactory.getSocketFactory() == null) {
            try {
                RMISocketFactory.setSocketFactory(new HostSocketFactory());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static ServiceManager getInstance() {
        return instance;
    }

    void init(Jmx jmx) throws Exception {
        objectName = new ObjectName("QingZhou:name=console");
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(new ConsoleJmx(), objectName);

        String jmxIp = jmx.getHost();
        if (Utils.isBlank(jmxIp)) {
            jmxIp = "0.0.0.0";
        }
        System.setProperty("java.rmi.server.hostname", jmxIp);

        int jmxPort = jmx.getPort();
        registry = LocateRegistry.createRegistry(jmxPort);

        String ipAndPort = jmxIp + ":" + jmxPort;
        String jmxServiceUrl = String.format("service:jmx:rmi://%s/jndi/rmi://%s/server", ipAndPort, ipAndPort);

        JMXServiceURL serviceURL = new JMXServiceURL(jmxServiceUrl);
        Map<String, Object> env = new HashMap<>();
        env.put(JMXConnectorServer.AUTHENTICATOR, new JMXAuthenticator() {
            @Override
            public Subject authenticate(Object credentials) {
                if (JmxServiceAdapterImpl.getInstance().authenticator == null) {
                    throw new SecurityException("jmx adapter not found");
                }
                return JmxServiceAdapterImpl.getInstance().authenticator.authenticate(credentials);
            }
        });
        env.put("jmx.remote.x.mlet.allow.getMBeansFromURL", "false");

        server = JMXConnectorServerFactory.newJMXConnectorServer(serviceURL, env, mBeanServer);
        server.setMBeanServerForwarder(new CustomMBeanServerAccessController());
        if (JmxServiceAdapterImpl.getInstance().notificationListener != null) {
            server.addNotificationListener(JmxServiceAdapterImpl.getInstance().notificationListener,
                    null, null);
        }
        server.start();
        System.out.println("Jmx service started: " + jmxServiceUrl);
    }

    void destroy() throws Exception {
        if (server != null) {
            mBeanServer.unregisterMBean(objectName);
            server.stop();
            server = null;
            UnicastRemoteObject.unexportObject(registry, true);
            System.out.println("Jmx service has stopped.");
        }
    }
}
