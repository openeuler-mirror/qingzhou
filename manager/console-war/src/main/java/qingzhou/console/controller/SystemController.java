package qingzhou.console.controller;

import org.apache.catalina.Manager;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.StandardContext;
import qingzhou.api.Response;
import qingzhou.api.type.List;
import qingzhou.api.type.Option;
import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.config.Security;
import qingzhou.console.ContextHelper;
import qingzhou.console.controller.jmx.JMXAuthenticatorImpl;
import qingzhou.console.controller.jmx.JmxInvokerImpl;
import qingzhou.console.controller.jmx.NotificationListenerImpl;
import qingzhou.console.login.LoginFreeFilter;
import qingzhou.console.login.LoginManager;
import qingzhou.console.login.vercode.VerCode;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.PairCipher;
import qingzhou.deployer.*;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.engine.util.pattern.FilterPattern;
import qingzhou.logger.Logger;
import qingzhou.registry.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

public class SystemController implements ServletContextListener, javax.servlet.Filter {
    public static Manager SESSIONS_MANAGER;
    private static String publicKey;
    public static PairCipher pairCipher;
    private static final ContextHelper contextHelper;

    static {
        contextHelper = ContextHelper.GetInstance.get();

        CryptoService cryptoService = SystemController.getService(CryptoService.class);
        Security security = getConsole().getSecurity();
        publicKey = security.getPublicKey();
        String privateKey = security.getPrivateKey();
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
        return getService(Deployer.class).getAppInfo(appName);
    }

    public static java.util.List<String> getAppInstances(String appName) {
        java.util.List<String> instances = new ArrayList<>();

        qingzhou.deployer.App app = getService(Deployer.class).getApp(appName);
        if (app != null) {
            instances.add(DeployerConstants.INSTANCE_LOCAL);
        }

        Registry registry = getService(Registry.class);
        instances.addAll(registry.getAppInstanceNames(appName));
        return instances;
    }

    public static ModelInfo getModelInfo(String appName, String model) {
        AppInfo appInfo = getAppInfo(appName);
        if (appInfo != null) return appInfo.getModelInfo(model);
        return null;
    }

    private static java.util.List<String> getAllIds(String app, String model, ModelFieldInfo fieldInfo) {
        java.util.List<String> idList = new ArrayList<>();

        if (!fieldInfo.isRequired()) {
            idList.add("");
        }

        RequestImpl req = new RequestImpl();
        req.setAppName(app);
        req.setModelName(model);
        req.setActionName(List.ACTION_ALL);
        Response res = getService(ActionInvoker.class).invokeSingle(req); // 续传
        if (res.isSuccess()) {
            for (Map<String, String> map : res.getDataList()) {
                idList.add(map.entrySet().iterator().next().getKey());
            }
        }
        return idList;
    }

    public static ItemInfo[] getOptions(String qzApp, ModelInfo modelInfo, String fieldName) {
        ItemInfo[] options = getOptions0(qzApp, modelInfo, fieldName);
        return options != null ? options : new ItemInfo[0];
    }

    private static ItemInfo[] getOptions0(String qzApp, ModelInfo modelInfo, String fieldName) {
        String[] dynamicOptionFields = modelInfo.getDynamicOptionFields();
        if (dynamicOptionFields != null) {
            for (String dynamicOptionField : dynamicOptionFields) {
                if (fieldName.equals(dynamicOptionField)) {
                    RequestImpl req = new RequestImpl();
                    req.setAppName(qzApp);
                    req.setModelName(modelInfo.getCode());
                    req.setActionName(Option.ACTION_OPTION);
                    req.setParameter(Option.FIELD_NAME_PARAMETER, fieldName);
                    ResponseImpl res = (ResponseImpl) getService(ActionInvoker.class).invokeSingle(req); // 续传
                    return res.getItemInfos();
                }
            }
        }

        String[] staticOptionFields = modelInfo.getStaticOptionFields();
        if (staticOptionFields != null) {
            for (String staticOptionField : staticOptionFields) {
                if (fieldName.equals(staticOptionField)) {
                    ItemInfo[] itemInfos = modelInfo.getOptionInfos().get(fieldName);
                    if (itemInfos != null && itemInfos.length > 0) {
                        return itemInfos;
                    }
                }
            }
        }

        ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(fieldName);
        String refModel = fieldInfo.getRefModel();
        if (Utils.notBlank(refModel)) {
            java.util.List<String> allIds = getAllIds(qzApp, refModel, fieldInfo);
            return allIds.stream().map(s -> new ItemInfo(s, new String[]{s, "en:" + s})).toArray(ItemInfo[]::new);
        }

        return new ItemInfo[0];
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

    private final Filter<SystemControllerContext>[] processors = new Filter[]{
            new TrustIpCheck(),
            new JspInterceptor(),
            new I18n(),
            new About(),
            new VerCode(),
            new LoginFreeFilter(),
            new LoginManager(),
            new Theme(),
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
