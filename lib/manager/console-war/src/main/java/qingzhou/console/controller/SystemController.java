package qingzhou.console.controller;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Objects;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Manager;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.StandardContext;
import qingzhou.api.InputType;
import qingzhou.api.Item;
import qingzhou.api.type.List;
import qingzhou.api.type.Option;
import qingzhou.config.console.Console;
import qingzhou.config.console.Security;
import qingzhou.config.console.impl.Config;
import qingzhou.console.controller.jmx.JmxAuthenticatorImpl;
import qingzhou.console.controller.jmx.JmxInvokerImpl;
import qingzhou.console.controller.jmx.NotificationListenerImpl;
import qingzhou.console.login.AuthManager;
import qingzhou.console.login.LoginManager;
import qingzhou.core.DeployerConstants;
import qingzhou.core.ItemData;
import qingzhou.core.console.ContextHelper;
import qingzhou.core.console.JmxServiceAdapter;
import qingzhou.core.deployer.*;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.registry.ModelFieldInfo;
import qingzhou.core.registry.ModelInfo;
import qingzhou.core.registry.Registry;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.PairCipher;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.engine.util.pattern.FilterPattern;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

public class SystemController implements ServletContextListener, javax.servlet.Filter {
    public static Manager SESSIONS_MANAGER;
    private static String publicKey;
    public static PairCipher pairCipher;
    private static final ContextHelper CONTEXT_HELPER;

    static {
        CONTEXT_HELPER = ContextHelper.GET_INSTANCE.get();

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

        AppManager appManager = getService(Deployer.class).getApp(appName);
        if (appManager != null) {
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

    public static ItemData[] getOptions(RequestImpl request, String fieldName) {
        ItemData[] options = getOptions0(request, fieldName);
        return options != null ? options : new ItemData[0];
    }

    private static ItemData[] getOptions0(RequestImpl request, String fieldName) {
        ModelInfo modelInfo = request.getCachedModelInfo();
        String[] dynamicOptionFields = modelInfo.getDynamicOptionFields();
        if (dynamicOptionFields != null) {
            for (String dynamicOptionField : dynamicOptionFields) {
                if (fieldName.equals(dynamicOptionField)) {
                    RequestImpl req = new RequestImpl(request);
                    req.setActionName(Option.ACTION_OPTION);
                    req.getParameters().put(DeployerConstants.DYNAMIC_OPTION_FIELD, fieldName);
                    ResponseImpl res = (ResponseImpl) getService(ActionInvoker.class).invokeAny(req); // 续传
                    return (ItemData[]) res.getInternalData();
                }
            }
        }

        String[] staticOptionFields = modelInfo.getStaticOptionFields();
        if (staticOptionFields != null) {
            for (String staticOptionField : staticOptionFields) {
                if (fieldName.equals(staticOptionField)) {
                    ItemData[] itemData = modelInfo.getOptionInfos().get(fieldName);
                    if (itemData != null && itemData.length > 0) {
                        return itemData;
                    }
                }
            }
        }

        ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(fieldName);
        String refModel = fieldInfo.getRefModel();
        if (Utils.notBlank(refModel)) {
            RequestImpl req = new RequestImpl(request);
            req.setModelName(refModel);
            req.setActionName(List.ACTION_ALL);

            ModelInfo refModelInfo = getAppInfo(request.getApp()).getModelInfo(refModel);
            req.setCachedModelInfo(refModelInfo);
            req.getParameters().put(DeployerConstants.LIST_ALL_ADD_FIELD, refModelInfo.getIdMaskField());

            ResponseImpl res = (ResponseImpl) getService(ActionInvoker.class).invokeAny(req); // 续传
            java.util.List<String[]> result = (java.util.List<String[]>) res.getInternalData();
            if (result != null && !result.isEmpty()) {
                return result.stream().map(s -> {
                    if (s.length == 1) {
                        return new ItemData(s[0], new String[]{s[0], "en:" + s[0]});
                    } else {
                        return new ItemData(s[0], new String[]{s[1], "en:" + s[1]});
                    }
                }).toArray(ItemData[]::new);
            }
        }

        InputType type = fieldInfo.getInputType();
        if (Objects.requireNonNull(type) == InputType.bool) {
            return new ItemData[]{
                    new ItemData(Item.of("true")),
                    new ItemData(Item.of("false"))
            };
        }

        return new ItemData[0];
    }

    public static <T> T getService(Class<T> type) {
        return getModuleContext().getService(type);
    }

    public static ModuleContext getModuleContext() {
        return CONTEXT_HELPER.getModuleContext();
    }

    public static Console getConsole() {
        Config fileConfig = new Config(getService(Json.class),
                new File(new File(getModuleContext().getInstanceDir(), "conf"), "qingzhou.json"));
        return fileConfig.getConsole();
    }

    public static String getPublicKeyString() {
        return publicKey;
    }

    private final Filter<SystemControllerContext>[] processors = new Filter[]{
            new TrustIpCheck(),
            new JspInterceptor(),
            new I18n(),
            new About(),
            new AuthManager(),
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
            forJmx(sce);
        } catch (Exception e) {
            getService(Logger.class).warn(e.getMessage(), e);
        }
    }

    private void forJmx(ServletContextEvent sce) throws Exception {
        JmxServiceAdapter jmxServiceAdapter = SystemController.getService(JmxServiceAdapter.class);
        if (jmxServiceAdapter == null) return;
        jmxServiceAdapter.registerJMXAuthenticator(new JmxAuthenticatorImpl());
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
        if (jmxServiceAdapter == null) return;
        jmxServiceAdapter.registerJMXAuthenticator(null);
        jmxServiceAdapter.registerJmxInvoker(null);
        jmxServiceAdapter.registerNotificationListener(null);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
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
