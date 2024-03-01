package qingzhou.console.jmx;

import org.apache.catalina.Manager;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.StandardContext;
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
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
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

    String CONSOLE_M_BEAN_NAME = "Qingzhou:name=console";
    private static JMXServerHolder instance;
    public static Manager manager;
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

    public JMXServerHolder() {
        instance = this;
    }

    public static synchronized JMXServerHolder getInstance() {
        if (instance == null) {
            return new JMXServerHolder();
        } else {
            return instance;
        }
    }

    public static String makeupJMXServiceUrl(String ip, int registerPort) {
        String ipAndPort = ip + ":" + registerPort;
        return String.format("service:jmx:rmi://%s/jndi/rmi://%s/server", ipAndPort, ipAndPort);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            init();
            ServletContext servletContext = sce.getServletContext();
            manager = getManager(servletContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Manager getManager(ServletContext servletContext) throws Exception {
        ApplicationContext context = null;
        if (servletContext instanceof ApplicationContextFacade) {
            Field field = servletContext.getClass().getDeclaredField("context");
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            context = (ApplicationContext) field.get(servletContext);
            field.setAccessible(accessible);
        } else if (servletContext instanceof ApplicationContext) {
            context = (ApplicationContext) servletContext;
        }

        if (context != null) {
            Field field = context.getClass().getDeclaredField("context");
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            StandardContext sc = (StandardContext) field.get(context);
            field.setAccessible(accessible);
            if (sc != null) {
                return sc.getManager();
            }
        }

        return null;
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
                HttpSession session = (HttpSession) manager.findSession(s[1]);
                if (session != null) {
                    session.invalidate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            destroy();
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
