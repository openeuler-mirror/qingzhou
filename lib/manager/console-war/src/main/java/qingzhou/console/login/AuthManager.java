package qingzhou.console.login;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import qingzhou.api.AuthAdapter;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.SystemControllerContext;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.core.deployer.Deployer;
import qingzhou.engine.util.pattern.Filter;

public class AuthManager implements Filter<SystemControllerContext> {
    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        Deployer deployer = SystemController.getService(Deployer.class);
        AuthAdapter authAdapter = deployer.getAuthAdapter();
        if (authAdapter == null) return true;

        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;
        String reqUri = RESTController.getReqUri(request);
        if (LoginManager.isOpenUris(reqUri)) return true; // 开放的 uri，不需要处理

        AuthContextImpl authContext = new AuthContextImpl(request, response);
        // 注销
        if (reqUri.equals(LoginManager.LOGIN_PATH)) {
            if (request.getParameter(LoginManager.LOGOUT_FLAG) != null) {
                LoginManager.logoutSession(request);
                authAdapter.logout(authContext);
                return false;
            }
        }

        // “认证”上下文
        authAdapter.doAuth(reqUri, authContext);

        // 根据是否认证通过，确定是否继续进入轻舟系统
        return authContext.isLoggedIn();
    }
}
