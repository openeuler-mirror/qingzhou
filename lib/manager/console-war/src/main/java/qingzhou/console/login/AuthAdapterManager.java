package qingzhou.console.login;

import qingzhou.api.AuthAdapter;
import qingzhou.config.User;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.SystemControllerContext;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.deployer.impl.AppImpl;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;

public class AuthAdapterManager implements Filter<SystemControllerContext> {
    private AuthAdapter authAdapter;
    private final ThreadLocal<AuthContextImpl> currentAuthAdapter = new ThreadLocal<>();

    {
        Deployer deployer = SystemController.getService(Deployer.class);
        for (String localApp : deployer.getLocalApps()) {
            AppImpl appImpl = (AppImpl) deployer.getApp(localApp);
            if ((authAdapter = appImpl.getAuthAdapter()) != null) break;
        }
    }

    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        if (authAdapter == null) return true;

        HttpServletRequest request = context.req;
        // 已登录
        if (LoginManager.getLoggedUser(request.getSession(false)) != null) return true;
        // 交由 LoginManager 处理：带了 LoginManager.LOGIN_USER 和 LoginManager.LOGIN_PASSWORD，很可能是要登录
        String reqUri = RESTController.getReqUri(request);
        if (reqUri.equals(LoginManager.LOGIN_URI)) return true;

        AuthContextImpl authContext = new AuthContextImpl(request::getParameter);
        currentAuthAdapter.set(authContext);
        authAdapter.authRequest(authContext);
        if (authContext.getAuthState() == AuthAdapter.AuthState.LOGGED_IN) {
            User user = buildUser(authContext.getUser());
            LoginManager.loginSession(context.req, user);
            return true;
        }
        return false;
    }

    private User buildUser(String username) {
        User user = new User();
        user.setName(username);
        user.setActive(true);
        user.setChangePwd(false);
        user.setEnableOtp(false);
        return user;
    }

    @Override
    public void afterFilter(SystemControllerContext context) {
        AuthContextImpl authContext = currentAuthAdapter.get();
        if (authContext == null) return;

        authAdapter.requestComplete(authContext);
        if (authContext.getAuthState() == AuthAdapter.AuthState.LOGGED_OUT) {
            LoginManager.logoutSession(context.req);
        }
    }
}
