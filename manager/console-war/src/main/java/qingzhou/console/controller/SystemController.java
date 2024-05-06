package qingzhou.console.controller;

import org.apache.catalina.Manager;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.StandardContext;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.config.ConfigService;
import qingzhou.config.Console;
import qingzhou.console.ContextHelper;
import qingzhou.console.i18n.SetI18n;
import qingzhou.console.jmx.JMXServerHolder;
import qingzhou.console.login.LoginFreeFilter;
import qingzhou.console.login.LoginManager;
import qingzhou.console.login.ResetPassword;
import qingzhou.console.login.vercode.VerCode;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.util.StringUtil;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.crypto.CryptoService;
import qingzhou.engine.util.crypto.CryptoServiceFactory;
import qingzhou.engine.util.crypto.KeyPairCipher;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.engine.util.pattern.FilterPattern;
import qingzhou.logger.Logger;
import qingzhou.registry.AppInfo;
import qingzhou.registry.Registry;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;

public class SystemController implements ServletContextListener, javax.servlet.Filter {
    public static Manager SESSIONS_MANAGER;
    private static String publicKey;
    private static String privateKey;
    public static KeyPairCipher keyPairCipher;
    private static ContextHelper contextHelper;

    static {
        contextHelper = ContextHelper.GetInstance.get();

        CryptoService cryptoService = CryptoServiceFactory.getInstance();
        publicKey = getConsole().getSecurity().getPublicKey();
        privateKey = getConsole().getSecurity().getPrivateKey();
        if (StringUtil.isBlank(publicKey) || StringUtil.isBlank(privateKey)) {
            String[] keyPair = cryptoService.generateKeyPair(UUID.randomUUID().toString().replace("-", ""));
            publicKey = keyPair[0];
            privateKey = keyPair[1];
        }
        try {
            keyPairCipher = cryptoService.getKeyPairCipher(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static AppInfo getAppInfo(String appName) {
        // 优先找本地，master和instance都在本地
        App app = getService(Deployer.class).getApp(appName);
        if (app != null) return app.getAppInfo();

        // 再找远程
        return getService(Registry.class).getAppInfo(appName);
    }

    public static <T> T getService(Class<T> type) {
        return contextHelper.getService(type);
    }

    public static ModuleContext getModuleContext() {
        return contextHelper.getModuleContext();
    }

    public static Console getConsole() {
        try {
            return getService(ConfigService.class).getModule().getConsole();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPublicKeyString() {
        return publicKey;
    }

    public static String getPrivateKeyString() {
        return privateKey;
    }

    public static void invokeLocalApp(Request request, Response response) throws Exception {
        getService(Deployer.class).getApp(PageBackendService.getAppName(request)).invokeDirectly(request, response);
    }

    private final Filter<HttpServletContext>[] processors = new Filter[]{
            new TrustedIPChecker(),
            new JspInterceptor(),
            new SetI18n(),
            new About(),
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
                ApplicationContext context = getApplicationContext(sce);
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
            getService(Logger.class).warn(e.getMessage(), e);
        }
    }

    private static ApplicationContext getApplicationContext(ServletContextEvent sce) throws NoSuchFieldException, IllegalAccessException {
        ServletContext servletContext = sce.getServletContext();
        if (servletContext instanceof ApplicationContextFacade) {
            Field field = servletContext.getClass().getDeclaredField("context");
            field.setAccessible(true);
            return (ApplicationContext) field.get(servletContext);
        } else if (servletContext instanceof ApplicationContext) {
            return (ApplicationContext) servletContext;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            JMXServerHolder.getInstance().destroy();
        } catch (Exception e) {
            getService(Logger.class).warn(e.getMessage(), e);
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
            getService(Logger.class).error(e.getMessage(), e);
        }
    }
}
