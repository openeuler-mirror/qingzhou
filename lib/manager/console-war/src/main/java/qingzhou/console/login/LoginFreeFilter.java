package qingzhou.console.login;

import javax.servlet.http.HttpServletRequest;

import qingzhou.console.controller.SystemControllerContext;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;

public class LoginFreeFilter implements Filter<SystemControllerContext> {
    private final String LOGIN_FREE_FLAG = "LOGIN_FREE_FLAG";

    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        HttpServletRequest request = context.req;

        // 已登录
        if (LoginManager.getLoginUser(request) != null) return true;

        // 交由 LoginManager 处理：带了 LoginManager.LOGIN_USER 和 LoginManager.LOGIN_PASSWORD，很可能是要登录
        String reqUri = RESTController.getReqUri(request);
        if (reqUri.equals(LoginManager.LOGIN_URI)) return true;

        if (Utils.notBlank(request.getParameter(LoginManager.LOGIN_USER))
                && Utils.notBlank(request.getParameter(LoginManager.LOGIN_PASSWORD))) {
            LoginManager.LoginFailedMsg loginFailedMsg = LoginManager.login(request);
            if (loginFailedMsg == null) {
                request.setAttribute(LOGIN_FREE_FLAG, LOGIN_FREE_FLAG);// 成功登录
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
