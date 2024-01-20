package qingzhou.framework.impl;

import qingzhou.framework.AppInfo;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.*;
import qingzhou.framework.impl.model.ModelInfo;
import qingzhou.framework.impl.model.ModelManagerImpl;
import qingzhou.framework.util.ClassLoaderUtil;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.ObjectUtil;
import qingzhou.framework.util.StringUtil;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class AppManagerImpl implements AppManager {
    private final FrameworkContext frameworkContext;
    private final Map<String, AppInfo> appInfoMap = new HashMap<>();

    public AppManagerImpl(FrameworkContext frameworkContext) {
        this.frameworkContext = frameworkContext;
    }

    private AppInfoImpl buildAppInfo(String appName, File appDir) {
        File[] listFiles = new File(appDir, "lib").listFiles();
        if (listFiles == null) {
            throw ExceptionUtil.unexpectedException("app lib not found: " + appName);
        }

        AppInfoImpl appInfo = new AppInfoImpl();

        AppContextImpl appContext = new AppContextImpl(frameworkContext);
        URLClassLoader loader = ClassLoaderUtil.newURLClassLoader(listFiles, QingZhouApp.class.getClassLoader());
        appInfo.setLoader(loader);
        ModelManager modelManager = buildModelManager(listFiles, loader);
        ConsoleContextImpl consoleContext = new ConsoleContextImpl();
        consoleContext.setModelManager(modelManager);
        appContext.setConsoleContext(consoleContext);
        for (String modelName : modelManager.getModelNames()) {
            ModelBase modelInstance = modelManager.getModelInstance(modelName);
            modelInstance.setAppContext(appContext);
            modelInstance.init();
        }
        appInfo.setAppContext(appContext);

        try (InputStream inputStream = loader.getResourceAsStream("app.properties")) {
            Properties properties = ObjectUtil.streamToProperties(inputStream);
            appInfo.setAppProperties(properties);
            String appClass = appInfo.getAppProperties().getProperty("qingzhou.app");
            if (StringUtil.notBlank(appClass)) {
                QingZhouApp qingZhouApp = (QingZhouApp) loader.loadClass(appClass).newInstance();
                appInfo.setQingZhouApp(qingZhouApp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return appInfo;
    }

    private ModelManagerImpl buildModelManager(File[] appLib, URLClassLoader loader) {
        ModelManagerImpl modelManager = new ModelManagerImpl();
        try {
            modelManager.init(appLib, loader);

            for (String modelName : modelManager.getModelNames()) {
                ModelInfo modelInfo = modelManager.getModelInfo(modelName);
                Class<?> modelClass = loader.loadClass(modelInfo.className);
                if (!ModelBase.class.isAssignableFrom(modelClass)) {
                    throw new IllegalArgumentException("The class annotated by the @Model ( " + modelClass.getName() + " ) needs to 'extends ModelBase'.");
                }
                modelInfo.setModelClass(modelClass);
                try {
                    modelInfo.setModelInstance((ModelBase) modelClass.newInstance());
                } catch (InstantiationException e) {
                    throw new IllegalArgumentException("The class annotated by the @Model needs to have a public parameter-free constructor.", e);
                }

                modelInfo.fieldInfoMap.forEach((s, fieldInfo) -> {
                    try {
                        Field field = modelClass.getField(fieldInfo.fieldName);
                        fieldInfo.setField(field);
                    } catch (NoSuchFieldException | SecurityException e) {
                        e.printStackTrace();
                    }
                });

                modelInfo.actionInfoMap.forEach((s, actionInfo) -> {
                    try {
                        Method method = modelClass.getMethod(actionInfo.methodName, Request.class, Response.class);
                        actionInfo.setJavaMethod(method);
                    } catch (NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                    }
                });
            }

            modelManager.initDefaultProperties();
        } catch (Exception e) {
            throw new IllegalArgumentException("The class annotated by the @Model needs to have a public parameter-free constructor.", e);
        }
        return modelManager;
    }

    @Override
    public void installApp(String appName, File appDir) throws Exception {
        if (appInfoMap.containsKey(appName)) {
            throw new IllegalArgumentException("The app already exists: " + appName);
        }

        AppInfoImpl appInfo = buildAppInfo(appName, appDir);
        appInfoMap.put(appName, appInfo);

        QingZhouApp qingZhouApp = appInfo.getQingZhouApp();
        if (qingZhouApp != null) {
            qingZhouApp.start(appInfo.getAppContext());
        }
    }

    @Override
    public void uninstallApp(String name) throws Exception {
        AppInfoImpl appInfo = (AppInfoImpl) appInfoMap.remove(name);
        if (appInfo == null) return;

        QingZhouApp qingZhouApp = appInfo.getQingZhouApp();
        if (qingZhouApp != null) {
            qingZhouApp.stop();
        }

        appInfo.getLoader().close();
    }

    @Override
    public Set<String> getApps() {
        return appInfoMap.keySet();
    }

    @Override
    public AppInfo getAppInfo(String name) {
        return appInfoMap.get(name);
    }
}
