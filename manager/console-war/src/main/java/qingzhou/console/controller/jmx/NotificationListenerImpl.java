package qingzhou.console.controller.jmx;

import qingzhou.console.controller.SystemController;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;
import javax.servlet.http.HttpSession;

public class NotificationListenerImpl implements NotificationListener {
    @Override
    public void handleNotification(Notification notification, Object handback) {
        try {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
