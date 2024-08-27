package qingzhou.console.jmx;

import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import qingzhou.console.controller.SystemController;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.view.ViewManager;

import javax.security.auth.Subject;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.util.*;

public class JmxHttpServletRequest implements HttpServletRequest {
    private final String appName;
    private final String modelName;
    private final String actionName;
    private final String id;
    private final Properties properties;
    private final Hashtable<String, Object> attrs = new Hashtable<>();
    private StandardSession session;

    public JmxHttpServletRequest(String appName, String modelName, String actionName, Properties properties) {
        this.appName = appName;
        this.modelName = modelName;
        this.actionName = actionName;
        this.properties = properties;
        String id = properties == null ? null : properties.getProperty("id");
        this.id = PageBackendService.encodeId(id);
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];
    }

    @Override
    public long getDateHeader(String s) {
        return 0;
    }

    @Override
    public String getHeader(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getHeaders(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return null;
    }

    @Override
    public int getIntHeader(String s) {
        return 0;
    }

    @Override
    public String getMethod() {
        return "POST";
    }

    @Override
    public String getPathInfo() {
        String uri = "/" + ViewManager.jsonView + "/" + "app" + "/" + appName + "/" + modelName + "/" + actionName;
        if (this.id != null) {
            uri = uri + "/" + id;
        }
        return uri;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        return "/console";
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String s) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        AccessControlContext context = AccessController.getContext();
        Subject subject = Subject.getSubject(context);
        if (subject != null) {
            Set<Principal> principals = subject.getPrincipals();
            Iterator<Principal> iterator = principals.iterator();
            if (iterator.hasNext()) {
                return iterator.next();
            }
        }
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        if (session == null) {
            return null;
        }
        return session.getId();
    }

    @Override
    public String getRequestURI() {
        return "/jmx" + getPathInfo();
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public String getServletPath() {
        return "/jmx";
    }

    @Override
    public HttpSession getSession(boolean b) {
        if (session != null && !session.isValid()) {
            session = null;
        }
        if (session != null) {
            return session;
        }
        Principal userPrincipal = getUserPrincipal();
        if (userPrincipal != null) {
            String id = userPrincipal.getName();
            try {
                session = (StandardSession) SystemController.SESSIONS_MANAGER.findSession(id);
            } catch (IOException ignored) {
            }
        }
        if (session != null && !session.isValid()) {
            session = null;
        }
        if (session == null && b) {
            session = new JmxStandardSession(SystemController.SESSIONS_MANAGER);
            session.addSessionListener(event -> {
                if (event.getType().equals(Session.SESSION_DESTROYED_EVENT)) {
                    Session session = event.getSession();
                    JMXServerHolder.getInstance().closeConnection(session.getId());
                }
            });
        }

        if (session != null) {
            session.access();
        }

        return session;
    }


    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        if (session == null) {
            return false;
        }
        return session.isValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) {
        return false;
    }

    @Override
    public void login(String s, String s1) throws ServletException {
    }

    @Override
    public void logout() {
    }

    @Override
    public Collection<Part> getParts() {
        return null;
    }

    @Override
    public Part getPart(String s) {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) {
        return null;
    }

    @Override
    public Object getAttribute(String s) {
        return attrs.get(s);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return attrs.keys();
    }

    @Override
    public String getCharacterEncoding() {
        return StandardCharsets.UTF_8.name();
    }

    @Override
    public void setCharacterEncoding(String s) {
    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public String getContentType() {
        return "application/x-www-form-urlencoded; charset=UTF-8";
    }

    @Override
    public ServletInputStream getInputStream() {
        return null;
    }

    @Override
    public String getParameter(String s) {
        return properties.getProperty(s);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return (Enumeration<String>) properties.propertyNames();
    }

    @Override
    public String[] getParameterValues(String s) {
        String value = properties.getProperty(s);
        return value == null ? null : new String[]{value};
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> map = new HashMap<>();
        for (String propertyName : properties.stringPropertyNames()) {
            String value = properties.getProperty(propertyName);
            map.put(propertyName, new String[]{value});
        }
        return map;
    }

    @Override
    public String getProtocol() {
        return "jmx";
    }

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public int getServerPort() {
        return -1;
    }

    @Override
    public BufferedReader getReader() {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        try {
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            return null;
        }
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public void setAttribute(String s, Object o) {
        if (o != null) {
            attrs.put(s, o);
        }
    }

    @Override
    public void removeAttribute(String s) {
        attrs.remove(s);
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    @Override
    public String getRealPath(String s) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return getSession().getServletContext();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }
}