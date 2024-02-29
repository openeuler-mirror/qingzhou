package qingzhou.console.login;

import qingzhou.console.controller.HttpServletContext;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.view.type.JsonView;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class LoginFreeFilter implements Filter<HttpServletContext> {
    public static final String LOGIN_FREE_FLAG = "LOGIN_FREE_FLAG";

    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;

        HttpSession session = request.getSession(false);
        if (session != null) {
            if (LoginManager.getLoginUser(session) != null) { // 已经登录
                return true;
            }
        }
        String checkPath = RESTController.retrieveServletPathAndPathInfo(request);
        if (checkPath.startsWith("/static/")) {
            for (String suffix : LoginManager.STATIC_RES_SUFFIX) {
                if (checkPath.endsWith(suffix)) {
                    return true;
                }
            }
        }

        if (checkPath.equals(LoginManager.LOGIN_URI)) { // 浏览器登录
            return true;
        }

        // rest 临时登录
        if (StringUtil.notBlank(request.getParameter(LoginManager.LOGIN_USER))
                && StringUtil.notBlank(request.getParameter(LoginManager.LOGIN_PASSWORD))) {
            try {
                LoginManager.LoginFailedMsg loginFailedMsg = LoginManager.login(request);
                if (loginFailedMsg == null) {
                    // 成功登录
                    request.setAttribute(LOGIN_FREE_FLAG, LOGIN_FREE_FLAG);
                    return true;
                } else {
                    String headerMsg = loginFailedMsg.getHeaderMsg();
                    String msg = LoginManager.retrieveI18nMsg(headerMsg);
                    JsonView.responseErrorJson(response, msg);
                }
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw e;
                } else {
                    throw new IllegalStateException(e);
                }
            }
            return false;
        }

        return true;
    }

    @Override
    public void afterFilter(HttpServletContext context) {
        HttpServletRequest request = context.req;
        if (request.getAttribute(LOGIN_FREE_FLAG) != null) {
            LoginManager.logout(context.req);
        }
    }
}
