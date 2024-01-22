package qingzhou.app.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.framework.App;
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
        }

        installNodeApp();

        installApps();
    }

    private void installMasterApp() throws Exception {
        logger.info("install master app");
        File masterApp = FileUtil.newFile(frameworkContext.getFileManager().getLib(), "sysapp", "master");
        AppImpl app = buildApp(masterApp);
        installApp(FrameworkContext.MASTER_APP_NAME, app);
    }

    private void installNodeApp() throws Exception {
        logger.info("install node app");
        File nodeApp = FileUtil.newFile(frameworkContext.getFileManager().getLib(), "sysapp", "node");
        AppImpl app = buildApp(nodeApp);
        installApp(FrameworkContext.NODE_APP_NAME, app);
    }

    private void installApps() throws Exception {
        File[] files = new File(frameworkContext.getFileManager().getDomain(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                String appName = file.getName();
                logger.info("install app: " + appName);
                AppImpl app = buildApp(file);
                installApp(appName, app);
            }
        }
    }

    private void installApp(String name, App app) throws Exception {
        appManager.installApp(name, app);
        if (frameworkContext.isMaster()) {
            frameworkContext.getAppStubManager().registerAppStub(name, app.getAppContext().getConsoleContext());
        }
    }

    @Override
    public void stop(BundleContext context) {
        String[] apps = appManager.getApps().toArray(new String[0]);
        Arrays.stream(apps).forEach(appName -> {
            try {
                AppImpl app = (AppImpl) appManager.uninstallApp(appName);
                if (app != null) {
                    app.getLoader().close();
                }
                logger.info("uninstall app: " + appName);
            } catch (Exception e) {
                logger.warn("failed to uninstall app: " + appName, e);
            }
        });
        context.ungetService(serviceReference);
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
