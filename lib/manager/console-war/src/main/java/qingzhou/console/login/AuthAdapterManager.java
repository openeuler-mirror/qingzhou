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
import javax.servlet.http.HttpServletResponse;

public class AuthAdapterManager implements Filter<SystemControllerContext> {
    private AuthAdapter authAdapter;

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
        HttpServletResponse response = context.resp;

        // 已登录
        if (LoginManager.getLoggedUser(request.getSession(false)) != null) return true;

        String reqUri = RESTController.getReqUri(request);
        if (LoginManager.isOpenUris(reqUri)) return true; // 开放的 uri，不需要处理

        // 准备“认证”过程的上下文环境
        AuthContextImpl authContext = new AuthContextImpl(request::getParameter);

        // 是否要监听特定的登录请求
        String listenUri = authAdapter.getLoginUri();
        if (listenUri == null || listenUri.equals(reqUri)) {
            boolean success = authAdapter.login(authContext);
            if (success) {
                String user = authContext.getUser();
                LoginManager.loginSession(request, buildUser(user));
                // 进入主页
                response.sendRedirect(RESTController.encodeURL(response, request.getContextPath() + LoginManager.INDEX_PATH)); // to welcome page
                return true;
            }
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
}
