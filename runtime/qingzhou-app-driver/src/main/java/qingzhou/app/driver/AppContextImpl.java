package qingzhou.app.driver;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.*;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import qingzhou.api.*;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.registry.AppStubLocal;

public class AppContextImpl implements AppContext {
    public final AppMeta appMeta;

    private final AppDriver appDriver;
    private final BundleContext bundleContext;
    private final File appTemp;

    Properties appProperties;
    String detectedPath;
    QingzhouApp qingzhouApp;
    final Map<Model, ModelBase> modelInstances = new HashMap<>();
    final Map<String, Method> actionMethods = new HashMap<>();

    private final File instanceFile = new File(System.getProperty("qingzhou.instance")); // 缓存，防止系统参数被应用覆盖
    private final String qzVersion = new File(System.getProperty("qingzhou.version")).getName().substring("version".length()); // 缓存，防止系统参数被应用覆盖

    // 应用启动过程中，可能被调用
    final List<ActionFilter> actionFilters = new ArrayList<>();

    private ServiceRegistration<AppStubLocal> appRegistration;

    private final Set<ServiceRegistration<?>> sharedFunctionServiceRegistrationList = new HashSet<>();
    private final Set<ServiceReference<?>> sharedFunctionServiceReferences = new HashSet<>();

    AppContextImpl(AppDriver appDriver, BundleContext bundleContext, AppMeta appMeta) {
        this.appDriver = appDriver;
        this.bundleContext = bundleContext;
        this.appMeta = appMeta;
        this.appTemp = Paths.get(getBase().getAbsolutePath(), "temp", "apps", appMeta.getApp().code).toFile();
    }

    void start() throws Exception {
        qingzhouApp.start(this);
        modelInstances.values().forEach(ModelBase::start);

        // 注册本地应用
        AppStubLocal appStub = new AppStubLocalImpl(this);
        appRegistration = bundleContext.registerService(AppStubLocal.class, appStub, null);
    }

    void stop() {
        appRegistration.unregister();
        sharedFunctionServiceRegistrationList.forEach(ServiceRegistration::unregister);
        sharedFunctionServiceReferences.forEach(bundleContext::ungetService);
        actionFilters.clear();

        modelInstances.values().forEach(ModelBase::stop);
        qingzhouApp.stop();
    }

    @Override
    public Properties getProperties() {
        return appProperties;
    }

    @Override
    public String getDetectedPath() {
        return detectedPath;
    }

    @Override
    public File getBase() {
        return instanceFile;
    }

    @Override
    public String getVersion() {
        return qzVersion;
    }

    @Override
    public void addActionFilter(ActionFilter... actionFilter) {
        actionFilters.addAll(Arrays.asList(actionFilter));
    }

    @Override
    public File getTemp() {
        return appTemp;
    }

    @Override
    public long getPid() {
        String name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        return Long.parseLong(name.split("@")[0]);
    }

    @Override
    public <T> T getService(Class<T> clazz) {
        return getService(clazz, null);
    }

    @Override
    public <T> T getService(Class<T> serviceType, String name) {
        return appDriver.getService(serviceType, name);
    }

    @Override
    public <T> T getObjectInstance(Class<T> type) {
        if (type != null) {
            for (ModelBase modelBase : modelInstances.values()) {
                if (type.isInstance(modelBase)) return (T) modelBase;
            }
            if (type.isInstance(qingzhouApp)) {
                return (T) qingzhouApp;
            }
        }
        return null;
    }

    @Override
    public <T, R> SharedFunctionRegistration registerSharedFunction(String functionName, SharedFunction<T, R> function) {
        if (functionName == null || functionName.isEmpty()) {
            throw new IllegalArgumentException("shared function name is required");
        }
        Dictionary<String, String> dictionary = new Hashtable<>();
        dictionary.put(Constants.SERVICE_PID, functionName);
        ServiceRegistration<?> serviceRegistration = bundleContext.registerService(SharedFunction.class, function, dictionary);
        sharedFunctionServiceRegistrationList.add(serviceRegistration);
        return () -> {
            sharedFunctionServiceRegistrationList.remove(serviceRegistration);
            try {
                serviceRegistration.unregister();
            } catch (IllegalStateException e) {
                if (!e.getMessage().contains("already unregistered")) {
                    throw e;
                }
            }
        };
    }

    @Override
    public <T, R> SharedFunction<T, R> getSharedFunction(String functionName) {
        try {
            Collection<ServiceReference<SharedFunction>> serviceReferences = bundleContext.getServiceReferences(SharedFunction.class,
                    "(" + Constants.SERVICE_PID + "=" + functionName + ")");
            if (!serviceReferences.isEmpty()) {
                ServiceReference<SharedFunction> serviceReference = serviceReferences.iterator().next();
                this.sharedFunctionServiceReferences.add(serviceReference);
                return bundleContext.getService(serviceReference);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return null;
    }
}
