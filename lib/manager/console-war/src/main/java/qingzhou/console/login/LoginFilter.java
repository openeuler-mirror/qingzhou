package qingzhou.console.login;

import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.SystemControllerContext;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.login.oauth2.Oauth2Login;
import qingzhou.core.LoginInterceptor;
import qingzhou.core.config.User;
import qingzhou.core.deployer.Deployer;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LoginFilter implements Filter<SystemControllerContext> {

    static {
        SystemController.getModuleContext().registerService(LoginInterceptor.class, new Oauth2Login());
    }

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

    private static Collection<LoginInterceptor> getLoginPlugins() {
        Deployer deployer = SystemController.getService(Deployer.class);
        List<String> allApp = deployer.getAllApp();
        Set<LoginInterceptor> loginServiceList = new HashSet<>();
        for (String app : allApp) {
            LoginInterceptor loginService = deployer.getApp(app).getAppContext().getService(LoginInterceptor.class);
            loginServiceList.add(loginService);
        }
        return loginServiceList;
    }


    // 加入原系统内部的验证机制
    private void mergeUserInfoAndRole(String username) {
        User user = new User();
        user.setName(username);
        user.setInfo(username);
        user.setActive(true);
        user.setChangePwd(false);
        user.setEnableOtp(false);

        // 并入内部的权限体系
        SystemController.getConsole().getSsoUsers().add(user);
    }
}
