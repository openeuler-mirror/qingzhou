package qingzhou.app.driver;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.*;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.api.ActionFilter;
import qingzhou.api.AppContext;
import qingzhou.api.ModelBase;
import qingzhou.api.QingzhouApp;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStubLocal;

class AppContextImpl implements AppContext {
    private final AppDriver appDriver;
    private final BundleContext bundleContext;
    private final AppMeta appMeta;
    private final File appTemp;

    Properties appProperties;
    QingzhouApp qingzhouApp;
    final Map<Model, ModelBase> modelInstances = new HashMap<>();
    final Map<String, Method> actionMethods = new HashMap<>();

    private final File instanceFile = new File(System.getProperty("qingzhou.instance")); // 缓存，防止系统参数被应用覆盖
    private final String qzVersion = new File(System.getProperty("qingzhou.version")).getName().substring("version".length()); // 缓存，防止系统参数被应用覆盖

    // 应用启动过程中，可能被调用
    final List<ActionFilter> actionFilters = new ArrayList<>();

    private Timer timer;
    private boolean started;
    private ServiceRegistration<AppStubLocal> appRegistration;

    AppContextImpl(AppDriver appDriver, BundleContext bundleContext, AppMeta appMeta) {
        this.appDriver = appDriver;
        this.bundleContext = bundleContext;
        this.appMeta = appMeta;
        this.appTemp = Paths.get(getBase().getAbsolutePath(), "temp", "apps", appMeta.getApp().code).toFile();
    }

    void start() {
        timer = new Timer("app-available");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (qingzhouApp.available(AppContextImpl.this)) {
                        start0();
                    } else {
                        stop0();
                    }
                } catch (Exception e) {
                    getService(Logger.class).error(e.getMessage(), e);
                }
            }
        }, 0, 1000 * Long.parseLong(appProperties.getProperty("interval", "60")));
    }

    void stop() {
        if (timer != null) {
            timer.cancel();
        }
        stop0();
    }

    private synchronized void start0() throws Exception {
        if (started) return;
        started = true;

        qingzhouApp.start(this);
        modelInstances.values().forEach(ModelBase::start);

        // 注册本地应用
        AppStubLocal appStub = new AppStubLocalImpl(this, appMeta);
        appRegistration = bundleContext.registerService(AppStubLocal.class, appStub, null);
    }

    private synchronized void stop0() {
        if (!started) return;
        started = false;

        appRegistration.unregister();
        actionFilters.clear();

        modelInstances.values().forEach(ModelBase::stop);
        qingzhouApp.stop();
    }

    @Override
    public Properties getProperties() {
        return appProperties;
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
}
