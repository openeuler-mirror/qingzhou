package qingzhou.app.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.app.impl.model.ModelInfo;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.*;
import qingzhou.framework.util.*;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Properties;

public class Controller implements BundleActivator {
    private ServiceReference<FrameworkContext> serviceReference;
    private FrameworkContext frameworkContext;
    private AppManager appManager;
    private Logger logger;

    @Override
    public void start(BundleContext context) throws Exception {
        serviceReference = context.getServiceReference(FrameworkContext.class);
        frameworkContext = context.getService(serviceReference);
        appManager = frameworkContext.getAppManager();
        logger = frameworkContext.getLogger();

        if (frameworkContext.isMaster()) {
            installMasterApp();
        } else {
            installNodeApp();
        }

        installApps();
    }

    private void installMasterApp() throws Exception {
        logger.info("install master app");
        File masterApp = FileUtil.newFile(frameworkContext.getLib(), "sysapp", "master");
        AppInfoImpl appInfo = buildAppInfo(masterApp);
        frameworkContext.getAppManager().installApp(FrameworkContext.MASTER_APP_NAME, appInfo);
    }

    private void installNodeApp() throws Exception {
        logger.info("install node app");
        File nodeApp = FileUtil.newFile(frameworkContext.getLib(), "sysapp", "node");
        AppInfoImpl appInfo = buildAppInfo(nodeApp);
        appManager.installApp(FrameworkContext.NODE_APP_NAME, appInfo);
    }

    private void installApps() throws Exception {
        File[] files = new File(frameworkContext.getDomain(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                String appName = file.getName();
                logger.info("install app: " + appName);
                AppInfoImpl appInfo = buildAppInfo(file);
                appManager.installApp(appName, appInfo);
            }
        }
    }

    @Override
    public void stop(BundleContext context) {
        String[] apps = appManager.getApps().toArray(new String[0]);
        Arrays.stream(apps).forEach(appName -> {
            try {
                AppInfoImpl appInfo = (AppInfoImpl) appManager.uninstallApp(appName);
                if (appInfo != null) {
                    appInfo.getLoader().close();
                }
                logger.info("uninstall app: " + appName);
            } catch (Exception e) {
                logger.warn("failed to uninstall app: " + appName, e);
            }
        });
        context.ungetService(serviceReference);
    }

    private AppInfoImpl buildAppInfo(File appDir) {
        File[] listFiles = new File(appDir, "lib").listFiles();
        if (listFiles == null) {
            throw ExceptionUtil.unexpectedException("app lib not found: " + appDir.getName());
        }

        AppInfoImpl appInfo = new AppInfoImpl();

        AppContextImpl appContext = new AppContextImpl(frameworkContext);
        URLClassLoader loader = ClassLoaderUtil.newURLClassLoader(listFiles, QingZhouApp.class.getClassLoader());
        appInfo.setLoader(loader);
        ConsoleContextImpl consoleContext = new ConsoleContextImpl();
        ModelManager modelManager = buildModelManager(listFiles, loader);
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
}
