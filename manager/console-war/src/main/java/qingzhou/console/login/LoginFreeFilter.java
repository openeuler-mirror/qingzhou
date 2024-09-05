package qingzhou.console.login;

import qingzhou.console.controller.SystemControllerContext;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;

public class LoginFreeFilter implements Filter<SystemControllerContext> {
    private final String LOGIN_FREE_FLAG = "LOGIN_FREE_FLAG";

    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        HttpServletRequest request = context.req;

        if (LoginManager.getLoginUser(request.getSession(false)) == null) {
            if (Utils.notBlank(request.getParameter(LoginManager.LOGIN_USER))
                    && Utils.notBlank(request.getParameter(LoginManager.LOGIN_PASSWORD))) {
                LoginManager.LoginFailedMsg loginFailedMsg = LoginManager.login(request);
                if (loginFailedMsg == null) {
                    request.setAttribute(LOGIN_FREE_FLAG, LOGIN_FREE_FLAG);// 成功登录
                }
            }
        }

        return true;
    }

    @Override
    public void afterFilter(SystemControllerContext context) {
        HttpServletRequest request = context.req;
        if (request.getAttribute(LOGIN_FREE_FLAG) != null) {
            LoginManager.logout(context.req);
        }
    }
}
