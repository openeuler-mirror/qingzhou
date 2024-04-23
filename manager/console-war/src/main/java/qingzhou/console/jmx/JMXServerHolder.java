package qingzhou.console.jmx;

import qingzhou.console.ServerXml;
import qingzhou.console.controller.SystemController;

import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnection;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIServerImpl;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JMXServerHolder {
    private static final JMXServerHolder instance = new JMXServerHolder();
    private JMXConnectorServer server;
    private ObjectName objectName;
    private MBeanServer mBeanServer;
    private Registry registry;

    static {
        if (RMISocketFactory.getSocketFactory() == null) {
            try {
                RMISocketFactory.setSocketFactory(new HostSocketFactory());
            } catch (IOException e) {
                SystemController.getLogger().warn(e.getMessage(), e);
            }
        }
    }

    public static JMXServerHolder getInstance() {
        return instance;
    }

    public boolean init() throws Exception {
        if (!ServerXml.get().isJmxEnabled()) {
            return false;
        }

        if (server != null) return false;

        objectName = new ObjectName("Qingzhou:name=console");
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
        if (!mBeanServer.isRegistered(objectName)) {
            mBeanServer.registerMBean(new ConsoleJmx(), objectName);
        }
        Map<String, String> jmxProp = ServerXml.get().jmx();
        String jmxIp = jmxProp.get("ip");
        System.setProperty("java.rmi.server.hostname", jmxIp);

        int jmxPort = Integer.parseInt(jmxProp.get("port"));
        registry = LocateRegistry.createRegistry(jmxPort);

        String ipAndPort = jmxIp + ":" + jmxPort;
        String jmxServiceUrl = String.format("service:jmx:rmi://%s/jndi/rmi://%s/server", ipAndPort, ipAndPort);

        JMXServiceURL serviceURL = new JMXServiceURL(jmxServiceUrl);
        Map<String, Object> env = new HashMap<>();
        env.put(JMXConnectorServer.AUTHENTICATOR, new CustomJMXAuthenticator());
        env.put("jmx.remote.x.mlet.allow.getMBeansFromURL", "false");

        server = JMXConnectorServerFactory.newJMXConnectorServer(serviceURL, env, mBeanServer);
        server.setMBeanServerForwarder(new CustomMBeanServerAccessController());
        server.addNotificationListener((notification, handback) -> {
            try {
                handleNotification(notification);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, null, null);
        server.start();
        System.out.println("Jmx service started: " + jmxServiceUrl);

        return true;
    }

    public void handleNotification(Notification notification) throws Exception {
        if (notification instanceof JMXConnectionNotification
                && notification.getType().equals(JMXConnectionNotification.CLOSED)) {
            JMXConnectionNotification jmxConnectionNotification = (JMXConnectionNotification) notification;
            String connectionId = jmxConnectionNotification.getConnectionId();
            String[] s = connectionId.split(" ");
            HttpSession session = (HttpSession) SystemController.SESSIONS_MANAGER.findSession(s[1]);
            if (session != null) {
                session.invalidate();
            }
        }
    }

    public void destroy() throws Exception {
        if (server != null) {
            mBeanServer.unregisterMBean(objectName);
            server.stop();
            server = null;
            UnicastRemoteObject.unexportObject(registry, true);
            System.out.println("Jmx service has stopped.");
        }
    }

    public void closeConnection(String sessionId) {
        try {
            if (server instanceof RMIConnectorServer) {
                String[] connectionIds = server.getConnectionIds();
                String connectionId = null;
                for (String id : connectionIds) {
                    String[] s = id.split(" ");
                    if (Objects.equals(s[1], sessionId)) {
                        connectionId = id;
                        break;
                    }
                }
                if (connectionId != null) {
                    RMIConnectorServer rmiConnectorServer = (RMIConnectorServer) server;
                    Class<? extends RMIConnectorServer> aClass = rmiConnectorServer.getClass();
                    Field field = aClass.getDeclaredField("rmiServerImpl");
                    field.setAccessible(true);
                    RMIServerImpl rmiServerImpl = (RMIServerImpl) field.get(rmiConnectorServer);
                    field.setAccessible(false);
                    Field f = RMIServerImpl.class.getDeclaredField("clientList");
                    f.setAccessible(true);
                    List<WeakReference<RMIConnection>> clientList = (List<WeakReference<RMIConnection>>) f.get(rmiServerImpl);
                    f.setAccessible(false);
                    RMIConnection rmiConnection = null;
                    for (WeakReference<RMIConnection> rmiConnectionWeakReference : clientList) {
                        rmiConnection = rmiConnectionWeakReference.get();
                        if (rmiConnection != null) {
                            if (connectionId.equals(rmiConnection.getConnectionId())) {
                                break;
                            }
                        }
                    }
                    if (rmiConnection != null) {
                        rmiConnection.close();
                    }
                }
            }
        } catch (Exception e) {
            SystemController.getLogger().warn("RMIConnection close failed! sessionId: " + sessionId, e);
        }
    }
}
