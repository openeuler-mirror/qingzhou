package qingzhou.agent.embedded.driver;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import qingzhou.agent.embedded.i18n.I18nService;
import qingzhou.api.*;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.logger.Logger;

public class AppContextImpl implements AppContext {
    private final AppMeta appMeta;
    private final URLClassLoader appClassLoader;
    private final File instanceDir;
    private final File appTemp;

    Properties appProperties = new Properties();
    QingzhouApp qingzhouApp;
    final Map<Model, ModelBase> modelInstances = new HashMap<>();
    final Map<String, Method> actionMethods = new HashMap<>();
    final List<ActionFilter> actionFilters = new ArrayList<>();

    private final Map<String, SharedFunction<?, ?>> sharedFunctions = new ConcurrentHashMap<>();

    private Timer timer;
    private boolean started;
    private Runnable onAppRegistered;
    private Runnable onAppUnregistered;

    public AppContextImpl(AppMeta appMeta, URLClassLoader appClassLoader, File instanceDir) {
        this.appMeta = appMeta;
        this.appClassLoader = appClassLoader;
        this.instanceDir = instanceDir;
        this.appTemp = new File(instanceDir, "temp/apps/" + appMeta.getApp().code);
        this.appTemp.mkdirs();
    }

    void setLifecycleCallbacks(Runnable onRegistered, Runnable onUnregistered) {
        this.onAppRegistered = onRegistered;
        this.onAppUnregistered = onUnregistered;
    }

    void start() {
        timer = new Timer("app-available", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (qingzhouApp.available()) {
                        start0();
                    } else {
                        stop0();
                    }
                } catch (Exception e) {
                    getService(Logger.class).error(e.getMessage(), e);
                }
            }
        }, 0, 60000);
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

        qingzhouApp.start();
        for (ModelBase modelBase : modelInstances.values()) {
            modelBase.start();
        }
        if (onAppRegistered != null) onAppRegistered.run();
    }

    private synchronized void stop0() {
        if (!started) return;
        started = false;

        if (onAppUnregistered != null) onAppUnregistered.run();
        actionFilters.clear();
        for (ModelBase modelBase : modelInstances.values()) {
            modelBase.stop();
        }
        qingzhouApp.stop();
    }

    @Override
    public QingzhouApp getAppInfo() {
        return qingzhouApp;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public File getBase() {
        return instanceDir;
    }

    @Override
    public File getTemp() {
        return appTemp;
    }

    @Override
    public String getPid() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }

    @Override
    public List<ActionFilter> getActionFilters() {
        return actionFilters;
    }

    public void addActionFilter(ActionFilter... filters) {
        actionFilters.addAll(Arrays.asList(filters));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> clazz) {
        return ServiceContainerProvider.getService(clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> clazz, String name) {
        return ServiceContainerProvider.getService(clazz, name);
    }

    @Override
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    public <T, R> SharedFunctionRegistration registerSharedFunction(String name, SharedFunction<T, R> function) {
        sharedFunctions.put(name, function);
        return () -> sharedFunctions.remove(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R> SharedFunction<T, R> getSharedFunction(String name) {
        return (SharedFunction<T, R>) sharedFunctions.get(name);
    }
}