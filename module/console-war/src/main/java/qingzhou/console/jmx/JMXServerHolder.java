package qingzhou.console.jmx;

import qingzhou.console.ServerXml;

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
import javax.servlet.ServletContextListener;
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

public class JMXServerHolder implements ServletContextListener {

    String CONSOLE_M_BEAN_NAME = "QingZhou:name=console";
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
                e.printStackTrace();
            }
        }
    }

    private JMXServerHolder() {
    }

    public static JMXServerHolder getInstance() {
        return instance;
    }

    public static String makeupJMXServiceUrl(String ip, int registerPort) {
        String ipAndPort = ip + ":" + registerPort;
        return String.format("service:jmx:rmi://%s/jndi/rmi://%s/server", ipAndPort, ipAndPort);
    }

    public synchronized void init() throws Exception {
        if (!ServerXml.get().isJmxEnabled()) {
            return;
        }
        if (server == null) {
            objectName = new ObjectName(CONSOLE_M_BEAN_NAME);
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
            if (!mBeanServer.isRegistered(objectName)) {
                mBeanServer.registerMBean(new JmxImpl(), objectName);
            }
            Map<String, String> jmxProp = ServerXml.get().jmx();
            String jmxIp = jmxProp.get("ip");
            System.setProperty("java.rmi.server.hostname", jmxIp);

            int jmxPort = Integer.parseInt(jmxProp.get("port"));
            registry = LocateRegistry.createRegistry(jmxPort);


            String jmxServiceUrl = makeupJMXServiceUrl(jmxIp, jmxPort);
            JMXServiceURL serviceURL = new JMXServiceURL(jmxServiceUrl);
            Map<String, Object> env = new HashMap<>();
            env.put(JMXConnectorServer.AUTHENTICATOR, new CustomJMXAuthenticator());
            env.put("jmx.remote.x.mlet.allow.getMBeansFromURL", "false");

            server = JMXConnectorServerFactory.newJMXConnectorServer(serviceURL, env, mBeanServer);
            server.setMBeanServerForwarder(new CustomMBeanServerAccessController());
            server.addNotificationListener(this::handleNotification, null, null);
            server.start();
            System.out.println("Jmx service started: " + jmxServiceUrl);
        }
    }

    public void handleNotification(Notification notification, Object handback) {
        try {
            if (notification instanceof JMXConnectionNotification
                    && notification.getType().equals(JMXConnectionNotification.CLOSED)) {
                JMXConnectionNotification jmxConnectionNotification = (JMXConnectionNotification) notification;
                String connectionId = jmxConnectionNotification.getConnectionId();
                String[] s = connectionId.split(" ");
                HttpSession session = (HttpSession) JmxHttpServletRequest.MANAGER.findSession(s[1]);
                if (session != null) {
                    session.invalidate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void destroy() throws Exception {
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
            System.out.println("RMIConnection close failed! sessionId: " + sessionId);
            e.printStackTrace();
        }
    }
}
