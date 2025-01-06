package qingzhou.console.login;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import qingzhou.api.AuthAdapter;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.SystemControllerContext;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.deployer.impl.AppManagerImpl;
import qingzhou.engine.util.pattern.Filter;

public class AuthManager implements Filter<SystemControllerContext> {
    private AuthAdapter authAdapter;

    {
        Deployer deployer = SystemController.getService(Deployer.class);
        for (String localApp : deployer.getLocalApps()) {
            AppManagerImpl appImpl = (AppManagerImpl) deployer.getApp(localApp);
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

        // “认证”上下文
        AuthContextImpl authContext = new AuthContextImpl(request::getParameter, request, response);
        authAdapter.doAuth(reqUri, authContext);
        return authContext.loginSuccessful; // 若登录成功，则继续业务
    }
}
