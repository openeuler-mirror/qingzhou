package qingzhou.console.jmx;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;

import java.io.IOException;

public class JmxStandardSession extends StandardSession {


    public JmxStandardSession(Manager manager) {
        super(manager);
        setNew(true);
        setValid(true);
        setCreationTime(System.currentTimeMillis());
        setMaxInactiveInterval(manager.getContext().getSessionTimeout() * 60);
        setId(generateSessionId());
    }


    protected String generateSessionId() {
        String id;
        do {
            try {
                id = manager.getSessionIdGenerator().generateSessionId();
                Session session = manager.findSession(id);
                if (session == null) {
                    return id;
                }
            } catch (IOException ignored) {
            }
        } while (true);
    }
}