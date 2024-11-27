package qingzhou.console.login;

import qingzhou.console.controller.SystemController;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionListener implements HttpSessionListener {
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        String username = (String) se.getSession().getAttribute(LoginManager.LOGIN_USER);
        if (username != null) {
            SystemController.getOnlineUser().removeUser(username);
        }
    }
}
