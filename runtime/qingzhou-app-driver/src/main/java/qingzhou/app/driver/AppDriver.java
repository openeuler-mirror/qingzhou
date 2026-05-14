package qingzhou.app.driver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import qingzhou.api.QingzhouApp;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.App;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStubLocal;

public class AppDriver implements BundleActivator {
    final File instanceFile = new File(System.getProperty("qingzhou.instance")); // 缓存，防止系统参数被应用覆盖
    final String qzVersion = System.getProperty("qingzhou.version"); // 缓存，防止系统参数被应用覆盖

    private BundleContext context;

    private final Set<ServiceReference<?>> serviceReferences = new HashSet<>();
    private final Map<String, Object> serviceObjects = new HashMap<>();

    private AppMeta appMeta;
    private AppContextImpl appContext;
    QingzhouApp qingzhouApp;

    private ServiceRegistration<AppStubLocal> appRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;

        // 准备元数据
        appMeta = new AppMeta();
        App app = parseAnnotations();
        appMeta.setApp(app);

        // 启动应用
        appContext = new AppContextImpl(this, appMeta);
        appContext.appProperties = getAppProperties(); // 通过 OSGI CM 监听
        String code = appContext.appProperties.getProperty("code");
        if (code != null && !code.trim().isEmpty()) {
            app.code = code.trim();
        }
        qingzhouApp = (QingzhouApp) Class.forName(app.className).newInstance();
        qingzhouApp.start(appContext);

        appContext.startModelInstances();

        // 注册本地应用
        AppStubLocal appStub = new AppStubLocalImpl(appContext, appMeta);
        appRegistration = context.registerService(AppStubLocal.class, appStub, null);
    }

    Properties getAppProperties() throws IOException {
        Properties appProperties = new Properties();
        ServiceReference<ConfigurationAdmin> serviceReference = context.getServiceReference(ConfigurationAdmin.class);
        ConfigurationAdmin configurationAdmin = context.getService(serviceReference);
        try {
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

    @Override
    public void stop(BundleContext context) {
        appRegistration.unregister();

        appContext.stopModelInstances();
        qingzhouApp.stop();

        serviceReferences.forEach(context::ungetService);
    }

    <T> T getService(Class<T> serviceType) {
        return getService(serviceType, null);
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
            for (int n; (n = inputStream.read(buffer)) != -1;) {
                bos.write(buffer, 0, n);
            }
            String json = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            Json jsonService = getService(Json.class);
            return jsonService.fromJson(json, App.class);
        }
    }
}
