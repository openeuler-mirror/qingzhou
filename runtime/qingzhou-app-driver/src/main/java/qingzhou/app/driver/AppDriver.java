package qingzhou.app.driver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import qingzhou.api.ModelBase;
import qingzhou.api.QingzhouApp;
import qingzhou.api.Request;
import qingzhou.common.FileUtil;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

public class AppDriver implements BundleActivator {
    private BundleContext context;
    private AppMeta appMeta;
    private AppContextImpl appContext;

    private final Set<ServiceReference<?>> serviceReferences = ConcurrentHashMap.newKeySet();
    private final Map<String, Object> serviceObjects = new ConcurrentHashMap<>();

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;

        // 初始化元数据
        appMeta = new AppMeta();
        appMeta.setApp(parseAnnotations());

        // 初始化上下文
        appContext = new AppContextImpl(this, context, appMeta);
        Properties appProperties = parseProperties();
        String code = appProperties.getProperty("code");
        if (code != null && !code.trim().isEmpty()) {
            appMeta.getApp().code = code.trim();
        }
        appContext.appProperties = appProperties;
        // 探测结果按 bundle 符号名分键传递，避免多 bundle 互相覆盖与清除竞态（见 QingzhouMain.startWithDetection）
        appContext.detectedPath = context.getProperty("Qingzhou-Detected-Path." + context.getBundle().getSymbolicName());

        // 初始化对象
        appContext.qingzhouApp = (QingzhouApp) Class.forName(appMeta.getApp().className).getDeclaredConstructor().newInstance();
        initAppModels();

        appContext.start();
    }

    @Override
    public void stop(BundleContext context) {
        appContext.stop();

        serviceReferences.forEach(context::ungetService);
        serviceReferences.clear();
        serviceObjects.clear(); // 缓存的服务包装实例随 bundle 停止失效，重复 start 须重新获取
    }

    <T> T getService(Class<T> serviceType, String name) {
        String cacheKey = serviceType.getName() + (name == null ? "" : "~" + name);
        Object cached = serviceObjects.get(cacheKey);
        if (cached != null) return serviceType.cast(cached);

        T service = loadService(serviceType, name);
        if (service != null) {
            // 只缓存命中结果：目标服务可能晚于首次调用注册，缓存 null 会让其永远不可见
            serviceObjects.put(cacheKey, service);
        }
        return service;
    }

    private <T> T loadService(Class<T> serviceType, String name) {
        boolean isLoggerService = serviceType == Logger.class;
        boolean isAppDefaultLoggerService = isLoggerService && name == null;
        String lookupName = isAppDefaultLoggerService ? "app_default" : name;

        ServiceReference<?> serviceReference = null;
        if (lookupName != null) {
            try {
                // name 既可能是服务自身的 service.pid（如共享函数），也可能是工厂配置 qingzhou-jdbc~h2 中 ~ 之后的实例名（此时 SCR 注册的 service.pid 为 qingzhou-jdbc~h2）。
                // 其中 *~ 是有意的通配前缀，只转义 name 本身
                String escapedName = escapeFilterValue(lookupName);
                Collection<ServiceReference<T>> serviceReferences = context.getServiceReferences(serviceType,
                        "(|(" + Constants.SERVICE_PID + "=" + escapedName + ")(" + Constants.SERVICE_PID + "=*~" + escapedName + "))");
                if (!serviceReferences.isEmpty()) {
                    serviceReference = serviceReferences.iterator().next();
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            serviceReference = context.getServiceReference(serviceType);
        }

        // 未配置 qingzhou-logger~app 时回退默认日志器
        if (isAppDefaultLoggerService && serviceReference == null) {
            serviceReference = context.getServiceReference(serviceType);
        }

        if (serviceReference != null) {
            serviceReferences.add(serviceReference);
            Object service = context.getService(serviceReference);
            // 服务已注册但对象不可得（如组件激活失败、服务被并发注销）：立即失败，
            // 避免把 null 包装成 Logger 交给应用，使其在调用处才以 NPE 暴露
            if (service == null) {
                throw new IllegalStateException("service object unavailable: " + serviceType.getName()
                        + (name == null ? "" : "~" + name));
            }
            if (isLoggerService) {
                service = new AppLogger(appMeta.getApp().code, (Logger) service);
            }
            return serviceType.cast(service);
        }
        return null;
    }

    // OSGi filter 为 LDAP 语法，值中的 \ * ( ) 必须转义：否则会破坏语法，
    // 或让 name 为 * 时匹配到任意服务，导致应用拿到错误的服务实例
    static String escapeFilterValue(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '*' || c == '(' || c == ')') {
                escaped.append('\\');
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    private App parseAnnotations() throws Exception {
        URL annotationFile = context.getBundle().getResource("/QZ-INF/annotation.json");
        if (annotationFile == null)
            throw new IllegalStateException("Missing /QZ-INF/annotation.json in bundle: " + context.getBundle().getSymbolicName());

        try (InputStream inputStream = annotationFile.openStream();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            FileUtil.copyStream(inputStream, bos);
            String json = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            Json jsonService = getService(Json.class, null);
            return jsonService.fromJson(json, App.class);
        }
    }

    private Properties parseProperties() throws IOException {
        Properties appProperties = new Properties();
        ServiceReference<ConfigurationAdmin> serviceReference = context.getServiceReference(ConfigurationAdmin.class);
        if (serviceReference == null) return appProperties; // ConfigurationAdmin 缺失时按无配置处理，避免 NPE
        try {
            ConfigurationAdmin configurationAdmin = context.getService(serviceReference);
            Configuration appConfiguration = configurationAdmin.getFactoryConfiguration("app",
                    context.getBundle().getSymbolicName(), null);
            Dictionary<String, Object> properties = appConfiguration.getProperties();
            if (properties != null) {
                Enumeration<?> keys = properties.keys();
                while (keys.hasMoreElements()) {
                    String key = (String) keys.nextElement();
                    if (key.equals(Constants.SERVICE_PID))
                        continue;
                    if (key.equals(ConfigurationAdmin.SERVICE_FACTORYPID))
                        continue;
                    Object value = properties.get(key);
                    if (value != null) { // 配置值不保证是 String，且 Hashtable 不接受 null 值
                        appProperties.setProperty(key, String.valueOf(value));
                    }
                }
            }
        } finally {
            context.ungetService(serviceReference);
        }
        return appProperties;
    }

    private void initAppModels() {
        appMeta.getApp().models.forEach(model -> {
            try {
                // 初始化模块实例
                Class<?> modelClass = Class.forName(model.className);
                ModelBase modelBase = (ModelBase) modelClass.getDeclaredConstructor().newInstance();
                modelBase.setAppContext(appContext);
                appContext.modelInstances.put(model, modelBase);

                // 初始化模块属性
                model.fields.forEach(modelField -> {
                    try {
                        Field field = modelBase.getClass().getField(modelField.code);
                        Object val = field.get(modelBase);
                        if (val != null) {
                            modelField.default_value = val.toString();
                        }
                    } catch (Throwable e) {
                        String msg = "failed to parse default value, model: " + model.code
                                + ", field: " + modelField.code + ", cause: " + e;
                        Logger logger = getService(Logger.class, null);
                        if (logger != null) {
                            logger.warn(msg);
                        } else { // Logger 服务未注册时降级 stderr，不掩盖原始解析异常
                            System.err.println(msg);
                        }
                    }
                });

                // 初始化模块操作
                model.actions.forEach(action -> {
                    try {
                        Method actionMethod = modelBase.getClass().getMethod(action.methodName, Request.class);
                        appContext.actionMethods.put(resolveActionKey(model, action), actionMethod);
                    } catch (Throwable ignored) {
                        // 实现的 Show List 等类型的默认方法不只有 Request.class 一个参数
                        // 他们在 invokeAction 中被委派到 DefaultAction 中处理
                    }
                });
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    static String resolveActionKey(Model model, ModelAction action) {
        return model.code + "->" + action.code;
    }
}
