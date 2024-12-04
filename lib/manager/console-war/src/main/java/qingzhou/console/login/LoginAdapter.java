package qingzhou.console.login;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import qingzhou.config.User;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.SystemControllerContext;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.core.LoginInterceptor;
import qingzhou.engine.util.pattern.Filter;

public class LoginAdapter implements Filter<SystemControllerContext> {
    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;

        String checkPath = RESTController.getReqUri(request);
        if (LoginManager.isOpenUris(checkPath)) {
            return true;
        }

        Collection<LoginInterceptor> loginServiceList = getLoginPlugins();
        if (request.getParameter(LoginManager.LOGOUT_FLAG) != null) {
            for (LoginInterceptor loginPlugin : loginServiceList) {
                LoginInterceptor.Result result = loginPlugin.logout(request, response);
                if (result != null) {
                    LoginManager.logout(request);

                    if (result.getRedirectUrl() != null) {
                        response.sendRedirect(result.getRedirectUrl());
                    }
                    return false;
                }
            }
            return true;
        }
        for (LoginInterceptor loginPlugin : loginServiceList) {
            try {
                LoginInterceptor.Result result = loginPlugin.login(request, response);
                if (result != null) {
                    if (result.getUsername() != null) {
                        mergeUserInfoAndRole(result.getUsername());
                        LoginManager.loginOk(request, result.getUsername());
                        loginPlugin.afterLogin(request, response);
                        // 进入主页
                        response.sendRedirect(RESTController.encodeURL(response, request.getContextPath() + LoginManager.INDEX_PATH)); // to welcome page
                    } else if (result.getRedirectUrl() != null) {
                        response.sendRedirect(result.getRedirectUrl());
                    }
                    return false;
                }
            } catch (Exception e) {
                String msg = "Login failed";
                response.setHeader(LoginManager.RESPONSE_HEADER_MSG_KEY, msg);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
                return false;
            }
        }
        return true;
    }

    // 加入原系统内部的验证机制
    private void mergeUserInfoAndRole(String username) {
        User user = new User();
        user.setName(username);
        user.setInfo(username);
        user.setActive(true);
        user.setChangePwd(false);
        user.setEnableOtp(false);

        // 记录 sso 登录的用户信息
        SystemController.getSsoUsers().put(username, user);
    }
}
