package qingzhou.app.impl;

import qingzhou.app.impl.filter.UniqueFilter;
import qingzhou.framework.*;
import qingzhou.framework.api.*;
import qingzhou.framework.util.ClassLoaderUtil;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.ObjectUtil;
import qingzhou.framework.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class AppManagerImpl implements AppManager, InternalService {
    private final Map<String, App> apps = new HashMap<>();
    private final FrameworkContext frameworkContext;

    public AppManagerImpl(FrameworkContext frameworkContext) {
        this.frameworkContext = frameworkContext;
    }

    @Override
    public void installApp(File appFile) throws Exception {
        String appName = appFile.getName();
        if (apps.containsKey(appName)) {
            throw new IllegalArgumentException("The app already exists: " + appName);
        }

        AppImpl app = buildApp(appFile);
        apps.put(appName, app);

        QingZhouApp qingZhouApp = app.getQingZhouApp();
        if (qingZhouApp != null) {
            qingZhouApp.start(app.getAppContext());
        }
    }

    @Override
    public void unInstallApp(String appName) throws Exception {
        AppImpl app = (AppImpl) apps.remove(appName);
        if (app != null) {
            try {
                app.getLoader().close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            QingZhouApp qingZhouApp = app.getQingZhouApp();
            if (qingZhouApp != null) {
                qingZhouApp.stop();
            }
        }
    }

    @Override
    public Set<String> getApps() {
        return apps.keySet();
    }

    @Override
    public App getApp(String name) {
        return apps.get(name);
    }


    private AppImpl buildApp(File appDir) {
        File[] listFiles = new File(appDir, "lib").listFiles();
        if (listFiles == null) {
            throw ExceptionUtil.unexpectedException("app lib not found: " + appDir.getName());
        }

        AppImpl app = new AppImpl();

        AppContextImpl appContext = new AppContextImpl(frameworkContext);
        URLClassLoader loader = ClassLoaderUtil.newURLClassLoader(listFiles, QingZhouApp.class.getClassLoader());
        app.setLoader(loader);
        ConsoleContextImpl consoleContext = new ConsoleContextImpl();
        ModelManager modelManager = buildModelManager(listFiles, loader);
        consoleContext.setModelManager(modelManager);
        appContext.setConsoleContext(consoleContext);
        appContext.addActionFilter(new UniqueFilter());
        for (String modelName : modelManager.getModelNames()) {
            ModelBase modelInstance = modelManager.getModelInstance(modelName);
            modelInstance.setAppContext(appContext);
            modelInstance.init();
        }
        app.setAppContext(appContext);

        try (InputStream inputStream = loader.getResourceAsStream("app.properties")) {
            Properties properties = ObjectUtil.streamToProperties(inputStream);
            app.setAppProperties(properties);
            String appClass = app.getAppProperties().getProperty("qingzhou.app");
            if (StringUtil.notBlank(appClass)) {
                QingZhouApp qingZhouApp = (QingZhouApp) loader.loadClass(appClass).newInstance();
                app.setQingZhouApp(qingZhouApp);
                if (qingZhouApp instanceof QingZhouSystemApp) {
                    ((QingZhouSystemApp) qingZhouApp).setFrameworkContext(frameworkContext);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return app;
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
}
