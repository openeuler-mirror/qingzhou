package qingzhou.app.impl;

import qingzhou.framework.AppDeployer;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.QingZhouSystemApp;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.api.QingZhouApp;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.util.ClassLoaderUtil;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.ObjectUtil;
import qingzhou.framework.util.StringUtil;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.Properties;

public class AppDeployerImpl implements AppDeployer {
    private final FrameworkContext frameworkContext;

    public AppDeployerImpl(FrameworkContext frameworkContext) {
        this.frameworkContext = frameworkContext;
    }

    @Override
    public void installApp(String name, File appFile) throws Exception {
        AppImpl app = buildApp(appFile);
        frameworkContext.getAppManager().addApp(name, app);

        if (frameworkContext.isMaster()) {
            frameworkContext.getAppStubManager().registerAppStub(name, frameworkContext.getAppManager().getApp(name).getAppContext().getConsoleContext());
        }
    }

    @Override
    public void unInstallApp(String name) throws Exception {
        frameworkContext.getAppManager().removeApp(name);
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
