package qingzhou.console.controller;

import org.apache.catalina.Manager;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.StandardContext;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.metadata.AppMetadata;
import qingzhou.console.AppMetadataManager;
import qingzhou.console.Controller;
import qingzhou.console.i18n.SetI18n;
import qingzhou.console.jmx.JMXServerHolder;
import qingzhou.console.login.LoginFreeFilter;
import qingzhou.console.login.LoginManager;
import qingzhou.console.login.ResetPassword;
import qingzhou.console.login.vercode.VerCode;
import qingzhou.console.page.PageBackendService;
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
import java.lang.reflect.Field;

public class SystemController implements ServletContextListener, javax.servlet.Filter {
    public static Manager SESSIONS_MANAGER;

    public static AppMetadata getAppMetadata(String appName) {
        return AppMetadataManager.getInstance().getAppMetadata(appName);
    }

    public static AppMetadata getAppMetadata(Request request) {
        return getAppMetadata(PageBackendService.getAppName(request));
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

    public static void invokeLocalApp(Request request, Response response) throws Exception {
        getAppManager().getApp(PageBackendService.getAppName(request)).invoke(request, response);
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
            (Filter<HttpServletContext>) context -> {
                context.chain.doFilter(context.req, context.resp); // 这样可以进入 servlet 资源
                return false;
            }
    };

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            boolean jmxStarted = JMXServerHolder.getInstance().init();
            if (jmxStarted) {
                ApplicationContext context;
                ServletContext servletContext = sce.getServletContext();
                if (servletContext instanceof ApplicationContextFacade) {
                    Field field = servletContext.getClass().getDeclaredField("context");
                    boolean accessible = field.isAccessible();
                    field.setAccessible(true);
                    context = (ApplicationContext) field.get(servletContext);
                    field.setAccessible(accessible);
                } else if (servletContext instanceof ApplicationContext) {
                    context = (ApplicationContext) servletContext;
                } else {
                    throw new IllegalStateException();
                }
                Field field = context.getClass().getDeclaredField("context");
                boolean accessible = field.isAccessible();
                field.setAccessible(true);
                StandardContext sc = (StandardContext) field.get(context);
                field.setAccessible(accessible);
                if (sc != null) {
                    SESSIONS_MANAGER = sc.getManager();
                } else {
                    throw new IllegalStateException();
                }
            }
        } catch (Exception e) {
            getLogger().warn(e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            JMXServerHolder.getInstance().destroy();
        } catch (Exception e) {
            getLogger().warn(e.getMessage(), e);
        }
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
}
