package qingzhou.console.controller.jmx;

import qingzhou.config.User;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.TrustIpCheck;
import qingzhou.console.login.LoginManager;
import qingzhou.core.DeployerConstants;
import qingzhou.engine.util.Utils;

import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.Properties;

public class JmxAuthenticatorImpl implements JMXAuthenticator {

    static {
        I18n.addKeyI18n("jmx.credentials.miss", new String[]{"请输入身份认证信息", "en:Please enter authentication information"});
        I18n.addKeyI18n("jmx.credentials.type.error", new String[]{"认证信息应为字符串数组类型，检测到不合法数据：%s", "en:Authentication information should be of type string array, invalid data detected: %s"});
        I18n.addKeyI18n("jmx.credentials.element.error", new String[]{"认证信息不完整，即字符串数组个数不足够", "en:The authentication information is incomplete, that is, the number of string arrays is insufficient"});
        I18n.addKeyI18n("jmx.credentials.element.isNull", new String[]{"用户名或密码不能为空", "en:The user name or password cannot be empty"});
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
            authenticationFailure(I18n.getKeyI18n("client.trusted.not"));
        }

        if (credentials == null) {
            authenticationFailure(I18n.getKeyI18n("jmx.credentials.miss"));
        }
        if (!(credentials instanceof String[])) {
            authenticationFailure(String.format(I18n.getKeyI18n("jmx.credentials.type.error"), credentials.getClass().getName()));
        }

        String[] aCredentials = (String[]) credentials;
        if (aCredentials.length < 2) {
            authenticationFailure(I18n.getKeyI18n("jmx.credentials.element.error"));
        }
        String username = aCredentials[0];
        String password = aCredentials[1];
        if (Utils.isBlank(username) || Utils.isBlank(password)) {
            authenticationFailure(I18n.getKeyI18n("jmx.credentials.element.isNull"));
        }

        JmxHttpServletRequest request = null;
        try {
            Properties jmxProperties = new Properties();
            jmxProperties.setProperty(LoginManager.LOGIN_USER, username);
            jmxProperties.setProperty(LoginManager.LOGIN_PASSWORD, password);
            if (aCredentials.length >= 3 && Utils.notBlank(aCredentials[2])) {
                jmxProperties.setProperty(DeployerConstants.LOGIN_OTP, aCredentials[2]);
            }

            request = new JmxHttpServletRequest("", "", "", jmxProperties);
            Object result = LoginManager.login(request::getParameter);
            if (result instanceof User) {
                LoginManager.loginSession(request, (User) result);
            } else {
                LoginManager.LoginFailedMsg loginFailedMsg = (LoginManager.LoginFailedMsg) result;
                String headerMsg = loginFailedMsg.getHeaderMsg();
                authenticationFailure(LoginManager.retrieveI18nMsg(headerMsg));
            }
        } catch (Exception e) {
            authenticationFailure(e.getMessage());
        }
        Subject subject = new Subject();
        String id = request.getSession().getId();
        subject.getPrincipals().add(new JMXPrincipal(id));
        return subject;
    }
}
