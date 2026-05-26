package qingzhou.agent.embedded.driver;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import qingzhou.api.ModelBase;
import qingzhou.api.QingzhouApp;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStubLocal;

public class AppLoader {
    private final AppMeta appMeta;
    private final AppContextImpl appContext;
    private final AppStubLocal appStub;
    private final URLClassLoader appClassLoader;
    private final Logger logger;

    public AppLoader(File appJar, AppMeta appMeta, File instanceDir, Logger logger) throws Exception {
        this.appMeta = appMeta;
        this.logger = logger;

        appClassLoader = new URLClassLoader(
                new URL[]{appJar.toURI().toURL()},
                getClass().getClassLoader()
        );

        this.appContext = new AppContextImpl(appMeta, appClassLoader, instanceDir);

        initApp();
        initModels();

        this.appStub = new AppStubLocalImpl(appContext, appMeta);
    }

    private void initApp() throws Exception {
        App app = appMeta.getApp();
        String className = app.className;
        logger.info("Loading app class: " + className);
        Class<?> appClass = appClassLoader.loadClass(className);
        appContext.qingzhouApp = (QingzhouApp) appClass.newInstance();
        appContext.qingzhouApp.setAppContext(appContext);
    }

    @SuppressWarnings("deprecation")
    private void initModels() throws Exception {
        App app = appMeta.getApp();
        for (Model modelMeta : app.models) {
            String className = modelMeta.className;
            logger.info("Loading model class: " + className);
            Class<?> modelClass = appClassLoader.loadClass(className);
            ModelBase modelBase = (ModelBase) modelClass.newInstance();
            modelBase.setAppContext(appContext);
            appContext.modelInstances.put(modelMeta, modelBase);

            // Set default field values
            for (ModelField fieldMeta : modelMeta.fields) {
                if (fieldMeta.default_value != null && !fieldMeta.default_value.isEmpty()) {
                    try {
                        Field field = modelClass.getField(fieldMeta.fieldName);
                        Object convertedValue = convertFieldValue(fieldMeta.default_value, field.getType());
                        field.set(modelBase, convertedValue);
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            }

            // Register custom action methods
            for (ModelAction actionMeta : modelMeta.actions) {
                if (!actionMeta.isDefaultAction) {
                    try {
                        Method m = modelClass.getMethod(actionMeta.methodName, qingzhou.api.Request.class);
                        String key = resolveActionKey(modelMeta, actionMeta);
                        appContext.actionMethods.put(key, m);
                    } catch (NoSuchMethodException e) {
                        logger.warn("Action method not found: " + modelMeta.code + "." + actionMeta.code + " -> " + actionMeta.methodName);
                    }
                }
            }
        }
    }

    static String resolveActionKey(Model model, ModelAction action) {
        return model.code + "->" + action.code;
    }

    private Object convertFieldValue(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == float.class || type == Float.class) return Float.parseFloat(value);
        return value;
    }

    public String getAppCode() {
        return appMeta.getApp().code;
    }

    public AppStubLocal getAppStub() {
        return appStub;
    }

    public AppContextImpl getAppContext() {
        return appContext;
    }

    public void start() {
        appContext.start();
    }

    public void stop() {
        appContext.stop();
    }

    public void close() {
        stop();
        try {
            appClassLoader.close();
        } catch (Exception ignored) {
        }
    }
}