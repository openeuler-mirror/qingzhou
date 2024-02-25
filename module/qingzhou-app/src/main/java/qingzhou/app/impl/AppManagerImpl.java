package qingzhou.app.impl;

import org.osgi.framework.BundleContext;
import qingzhou.api.*;
import qingzhou.app.App;
import qingzhou.app.AppManager;
import qingzhou.app.QingZhouSystemApp;
import qingzhou.framework.Framework;
import qingzhou.framework.util.*;

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

public class AppManagerImpl implements AppManager {
    private final Map<String, App> apps = new HashMap<>();
    private final Framework framework;
    private final BundleContext bundleContext;

    public AppManagerImpl(Framework framework, BundleContext bundleContext) {
        this.framework = framework;
        this.bundleContext = bundleContext;
    }

    @Override
    public void installApp(File appFile) throws Exception {
        String appName = appFile.getName();
        if (apps.containsKey(appName)) {
            throw new IllegalArgumentException("The app already exists: " + appName);
        }
        boolean needCommonApp = !qingzhou.app.App.SYS_APP_MASTER.equals(appName) && !qingzhou.app.App.SYS_APP_NODE_AGENT.equals(appName);
        AppImpl app = buildApp(appName, appFile, needCommonApp);
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
                Controller.logger.warn("failed to close loader: " + appName, e);
            }

            QingZhouApp qingZhouApp = app.getQingZhouApp();
            if (qingZhouApp != null) {
                File temp = app.getAppContext().getTemp();
                qingZhouApp.stop();
                FileUtil.forceDelete(temp);
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

    private AppImpl buildApp(String appName, File appDir, boolean needCommonApp) throws Exception {
        File[] appFiles = appDir.listFiles();
        if (appFiles == null) {
            throw ExceptionUtil.unexpectedException("app lib not found: " + appDir.getName());
        }
        File[] appLibs = appFiles;
        if (needCommonApp) {
            File[] commonFiles = FileUtil.newFile(framework.getLib(), "module", "qingzhou-app", "common").listFiles();
            if (commonFiles != null) {
                int appFileLength = appFiles.length;
                int commonFileLength = commonFiles.length;
                appLibs = new File[appFileLength + commonFileLength];
                System.arraycopy(appFiles, 0, appLibs, 0, appFileLength);
                System.arraycopy(commonFiles, 0, appLibs, appFileLength, commonFileLength);
            }
        }

        AppImpl app = new AppImpl();

        AppContextImpl appContext = new AppContextImpl(framework);
        appContext.setAppName(appName);

        URLClassLoader loader = ClassLoaderUtil.newURLClassLoader(appLibs, QingZhouApp.class.getClassLoader());
        app.setLoader(loader);
        ConsoleContextImpl consoleContext = new ConsoleContextImpl();
        ModelManager modelManager = buildModelManager(appLibs, loader);
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
                    QingZhouSystemApp qingZhouSystemApp = (QingZhouSystemApp) qingZhouApp;
                    qingZhouSystemApp.setFrameworkContext(framework);
                    qingZhouSystemApp.setBundleContext(bundleContext);
                }
            }
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

                for (FieldInfo fieldInfo : modelInfo.fieldInfoMap.values()) {
                    Field field = modelClass.getField(fieldInfo.fieldName);
                    fieldInfo.setField(field);
                }

                for (ActionInfo actionInfo : modelInfo.actionInfoMap.values()) {
                    Method method = modelClass.getMethod(actionInfo.methodName, Request.class, Response.class);
                    actionInfo.setJavaMethod(method);
                }
            }

            modelManager.initDefaultProperties();
        } catch (Exception e) {
            throw new IllegalArgumentException("The class annotated by the @Model needs to have a public parameter-free constructor.", e);
        }
        return modelManager;
    }
}
