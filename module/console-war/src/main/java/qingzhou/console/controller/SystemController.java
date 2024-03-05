package qingzhou.console.controller;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.metadata.AppMetadata;
import qingzhou.console.AppMetadataManager;
import qingzhou.console.Controller;
import qingzhou.console.login.LoginFreeFilter;
import qingzhou.console.login.LoginManager;
import qingzhou.console.login.ResetPassword;
import qingzhou.console.login.vercode.VerCode;
import qingzhou.framework.app.App;
import qingzhou.framework.app.AppManager;
import qingzhou.framework.config.Config;
import qingzhou.framework.crypto.CryptoService;
import qingzhou.framework.logger.Logger;
import qingzhou.framework.serializer.Serializer;
import qingzhou.framework.util.pattern.Filter;
import qingzhou.framework.util.pattern.FilterPattern;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class SystemController implements ServletContextListener, javax.servlet.Filter {
    private final Filter<HttpServletContext>[] processors = new Filter[]{
            new TrustedIPChecker(),
            new JspInterceptor(),
            new SetI18n(),
            new About(),
            new NodeRegister(),
            new VerCode(),
            new LoginFreeFilter(),
            new LoginManager(),
            new ResetPassword(),
            new AccessControl(),
            new SearchFilter(),
            new LastDecision()
    };

    public static AppMetadata getAppMetadata(String appName) {
        return AppMetadataManager.getInstance().getAppStub(appName);
    }

    public static Config getConfig() {
        return Controller.framework.getServiceManager().getService(Config.class);
    }

    public static AppManager getAppManager() {
        return Controller.framework.getServiceManager().getService(AppManager.class);
    }

    public static App getLocalApp(String appName) {
        return getAppManager().getApp(appName);
    }

    public static void invokeLocalApp(String appName, Request request, Response response) throws Exception {
        getAppManager().getApp(appName).invoke(request, response);
    }

    public static Serializer getSerializer() {
        return Controller.framework.getServiceManager().getService(Serializer.class);
    }

    public static CryptoService getCryptoService() {
        return Controller.framework.getServiceManager().getService(CryptoService.class);
    }

    public static Logger getLogger() {
        return Controller.framework.getServiceManager().getService(Logger.class);
    }

    public static File getCache(String subName) {
        return Controller.framework.getTemp(subName);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        HttpServletContext context = new HttpServletContext(httpServletRequest, httpServletResponse, chain);
        try {
            FilterPattern.doFilter(context, processors);
        } catch (Throwable e) {
            getLogger().error(e.getMessage(), e);
        }
    }

    private static final class LastDecision implements Filter<HttpServletContext> {

        @Override
        public boolean doFilter(HttpServletContext context) throws Exception {
            context.chain.doFilter(context.req, context.resp); // 这样可以进入 servlet 资源
            return false;
        }
    }
}
