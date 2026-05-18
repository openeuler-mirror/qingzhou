package qingzhou.app.driver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import qingzhou.api.ModelBase;
import qingzhou.api.QingzhouApp;
import qingzhou.api.Request;
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

    private final Set<ServiceReference<?>> serviceReferences = new HashSet<>();
    private final Map<String, Object> serviceObjects = new HashMap<>();

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

        // 初始化对象
        appContext.qingzhouApp = (QingzhouApp) Class.forName(appMeta.getApp().className).newInstance();
        initAppModels();

        appContext.start();
    }

    @Override
    public void stop(BundleContext context) {
        appContext.stop();

        serviceReferences.forEach(context::ungetService);
    }

    <T> T getService(Class<T> serviceType, String name) {
        Object found = serviceObjects.computeIfAbsent(serviceType.getName() + (name == null ? "" : "~" + name), c -> {
            ServiceReference<?> serviceReference = null;
            if (name != null) {
                try {
                    Collection<ServiceReference<T>> serviceReferences = context.getServiceReferences(serviceType,
                            "(" + Constants.SERVICE_PID + "=" + name + ")");
                    if (!serviceReferences.isEmpty()) {
                        serviceReference = serviceReferences.iterator().next();
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            } else {
                serviceReference = context.getServiceReference(serviceType);
            }
            if (serviceReference != null) {
                serviceReferences.add(serviceReference);
                Object service = context.getService(serviceReference);
                if (service instanceof Logger) {
                    service = new AppLogger(appMeta.getApp().code, (Logger) service);
                }
                return service;
            }
            return null;
        });

        return (T) found;
    }

    private App parseAnnotations() throws Exception {
        URL annotationFile = context.getBundle().getResource("/QZ-INF/annotation.json");
        try (InputStream inputStream = annotationFile.openStream()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024 * 4];
            for (int n; (n = inputStream.read(buffer)) != -1; ) {
                bos.write(buffer, 0, n);
            }
            String json = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            Json jsonService = getService(Json.class, null);
            return jsonService.fromJson(json, App.class);
        }
    }

    private Properties parseProperties() throws IOException {
        Properties appProperties = new Properties();
        ServiceReference<ConfigurationAdmin> serviceReference = context.getServiceReference(ConfigurationAdmin.class);
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
                    String value = (String) properties.get(key);
                    appProperties.setProperty(key, value);
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
                ModelBase modelBase = (ModelBase) modelClass.newInstance();
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
                    } catch (Throwable ignored) {
                        getService(Logger.class, null).warn("failed to parse default value, model: " + model.code + ", field: " + modelField.code);
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
