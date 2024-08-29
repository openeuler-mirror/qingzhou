package qingzhou.console.controller.jmx;

import qingzhou.console.ConsoleConstants;
import qingzhou.console.controller.TrustIpCheck;
import qingzhou.console.i18n.ConsoleI18n;
import qingzhou.console.i18n.I18n;
import qingzhou.console.login.LoginManager;
import qingzhou.engine.util.Utils;

import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.Properties;

public class JMXAuthenticatorImpl implements JMXAuthenticator {

    static {
        ConsoleI18n.addI18n("jmx.credentials.miss", new String[]{"请输入身份认证信息", "en:Please enter authentication information"});
        ConsoleI18n.addI18n("jmx.credentials.type.error", new String[]{"认证信息应为字符串数组类型，检测到不合法数据：%s", "en:Authentication information should be of type string array, invalid data detected: %s"});
        ConsoleI18n.addI18n("jmx.credentials.element.error", new String[]{"认证信息不完整，即字符串数组个数不足够", "en:The authentication information is incomplete, that is, the number of string arrays is insufficient"});
        ConsoleI18n.addI18n("jmx.credentials.element.isNull", new String[]{"用户名或密码不能为空", "en:The user name or password cannot be empty"});
        ConsoleI18n.addI18n("jmx.authentication.invalid", new String[]{"JMX 认证无效", "en:JMX authentication is invalid"});
    }

    private static void authenticationFailure(String message) throws SecurityException {
        throw new SecurityException(message);
    }

    @Override
    public Subject authenticate(Object credentials) {
        String clientHost;
        try {
            clientHost = RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            throw new RuntimeException(e);
        }
        if (TrustIpCheck.notTrustedIp(clientHost)) {
            authenticationFailure(ConsoleI18n.getI18n(I18n.getI18nLang(), "client.trusted.not"));
        }

        if (credentials == null) {
            authenticationFailure(ConsoleI18n.getI18n(I18n.getI18nLang(), "jmx.credentials.miss"));
        }
        if (!(credentials instanceof String[])) {
            authenticationFailure(String.format(ConsoleI18n.getI18n(I18n.getI18nLang(), "jmx.credentials.type.error"), credentials.getClass().getName()));
        }

        String[] aCredentials = (String[]) credentials;
        if (aCredentials.length < 2) {
            authenticationFailure(ConsoleI18n.getI18n(I18n.getI18nLang(), "jmx.credentials.element.error"));
        }
        String username = aCredentials[0];
        String password = aCredentials[1];
        if (Utils.isBlank(username) || Utils.isBlank(password)) {
            authenticationFailure(ConsoleI18n.getI18n(I18n.getI18nLang(), "jmx.credentials.element.isNull"));
        }

        JmxHttpServletRequest request = null;
        LoginManager.LoginFailedMsg loginFailedMsg = null;
        try {
            Properties jmxProperties = new Properties();
            jmxProperties.setProperty(LoginManager.LOGIN_USER, username);
            jmxProperties.setProperty(LoginManager.LOGIN_PASSWORD, password);
            if (aCredentials.length >= 3 && Utils.notBlank(aCredentials[2])) {
                jmxProperties.setProperty(ConsoleConstants.LOGIN_OTP, aCredentials[2]);
            }

            request = new JmxHttpServletRequest("", "", "", jmxProperties);
            loginFailedMsg = LoginManager.login(request);
        } catch (Exception e) {
            authenticationFailure(e.getMessage());
        }
        if (loginFailedMsg != null) {
            String headerMsg = loginFailedMsg.getHeaderMsg();
            authenticationFailure(LoginManager.retrieveI18nMsg(headerMsg));
        }
        Subject subject = new Subject();
        String id = request.getSession().getId();
        subject.getPrincipals().add(new JMXPrincipal(id));
        return subject;
    }
}
