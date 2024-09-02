package qingzhou.console.controller;

import org.apache.catalina.Manager;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.StandardContext;
import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.config.Security;
import qingzhou.console.ContextHelper;
import qingzhou.console.controller.jmx.JMXAuthenticatorImpl;
import qingzhou.console.controller.jmx.JmxInvokerImpl;
import qingzhou.console.controller.jmx.NotificationListenerImpl;
import qingzhou.console.login.LoginFreeFilter;
import qingzhou.console.login.LoginManager;
import qingzhou.console.login.ResetPassword;
import qingzhou.console.login.vercode.VerCode;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.PairCipher;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.JmxServiceAdapter;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.engine.util.pattern.FilterPattern;
import qingzhou.logger.Logger;
import qingzhou.registry.AppInfo;
import qingzhou.registry.ModelInfo;
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
    public static PairCipher pairCipher;
    private static final ContextHelper contextHelper;

    static {
        contextHelper = ContextHelper.GetInstance.get();

        CryptoService cryptoService = SystemController.getService(CryptoService.class);
        Security security = getConsole().getSecurity();
        publicKey = security.getPublicKey();
        privateKey = security.getPrivateKey();
        if (Utils.isBlank(publicKey) || Utils.isBlank(privateKey)) {
            String[] pairKey = cryptoService.generatePairKey();
            publicKey = pairKey[0];
            privateKey = pairKey[1];
        }
        try {
            pairCipher = cryptoService.getPairCipher(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getKeySize() { // login.jsp 使用
        return 1024;
    }

    public static String decryptWithConsolePrivateKey(String input, boolean ignoredEx) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        try {
            return SystemController.pairCipher.decryptWithPrivateKey(input);
        } catch (Exception e) {
            if (!ignoredEx) {
                SystemController.getService(Logger.class).warn("Decryption error", e);
            }
            return input;
        }
    }

    public static AppInfo getAppInfo(String appName) {
        // 优先找本地，master和instance都在本地
        App app = getService(Deployer.class).getApp(appName);
        if (app != null) return app.getAppInfo();

        // 再找远程
        return getService(Registry.class).getAppInfo(appName);
    }

    public static ModelInfo getModelInfo(String appName, String model) {
        AppInfo appInfo = getAppInfo(appName);
        if (appInfo != null) return appInfo.getModelInfo(model);
        return null;
    }

    public static <T> T getService(Class<T> type) {
        return getModuleContext().getService(type);
    }

    public static ModuleContext getModuleContext() {
        return contextHelper.getModuleContext();
    }

    public static Console getConsole() {
        return getService(Config.class).getConsole();
    }

    public static String getPublicKeyString() {
        return publicKey;
    }

    public static String getPrivateKeyString() {
        return privateKey;
    }

    private final Filter<SystemControllerContext>[] processors = new Filter[]{
            new TrustIpCheck(),
            new JspInterceptor(),
            new I18n(),
            new About(),
            new VerCode(),
            new LoginFreeFilter(),
            new LoginManager(),
            new ResetPassword(),
            new SecurityFilter(),
            (Filter<SystemControllerContext>) context -> {
                context.chain.doFilter(context.req, context.resp); // 这样可以进入 servlet 资源
                return false;
            }
    };

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            JmxServiceAdapter jmxServiceAdapter = SystemController.getService(JmxServiceAdapter.class);
            jmxServiceAdapter.registerJMXAuthenticator(new JMXAuthenticatorImpl());
            jmxServiceAdapter.registerJmxInvoker(new JmxInvokerImpl());
            jmxServiceAdapter.registerNotificationListener(new NotificationListenerImpl());

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
        JmxServiceAdapter jmxServiceAdapter = SystemController.getService(JmxServiceAdapter.class);
        jmxServiceAdapter.registerJMXAuthenticator(null);
        jmxServiceAdapter.registerJmxInvoker(null);
        jmxServiceAdapter.registerNotificationListener(null);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        SystemControllerContext context = new SystemControllerContext(httpServletRequest, httpServletResponse, chain);
        try {
            FilterPattern.doFilter(context, processors);
        } catch (Throwable e) {
            getService(Logger.class).error(e.getMessage(), e);
        }
    }
}
