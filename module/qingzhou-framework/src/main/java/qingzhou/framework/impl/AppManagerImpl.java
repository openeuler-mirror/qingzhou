package qingzhou.framework.impl;

import qingzhou.framework.AppInfo;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContextAware;
import qingzhou.framework.api.*;
import qingzhou.framework.impl.model.ModelInfo;
import qingzhou.framework.impl.model.ModelManagerImpl;
import qingzhou.framework.util.ClassLoaderUtil;
import qingzhou.framework.util.FileUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.*;

public class AppManagerImpl implements AppManager {
    private final Map<String, AppInfo> appInfoMap = new HashMap<>();

    private AppInfoImpl buildAppInfo(String appName, File[] appLib) {
        AppInfoImpl appInfo = new AppInfoImpl();

        AppContextImpl appContext = new AppContextImpl(FrameworkContextImpl.getFrameworkContext());
        appContext.setAppName(appName);
        URLClassLoader loader = ClassLoaderUtil.newURLClassLoader(appLib, QingZhouApp.class.getClassLoader());
        appInfo.setLoader(loader);
        ModelManager modelManager = buildModelManager(appLib, loader);
        appContext.setModelManager(modelManager);
        appContext.setConsoleContext(new ConsoleContextImpl(modelManager));
        for (String modelName : modelManager.getModelNames()) {
            ModelBase modelInstance = modelManager.getModelInstance(modelName);
            modelInstance.setAppContext(appContext);
            modelInstance.init();
        }
        appInfo.setAppContext(appContext);

        List<QingZhouApp> apps = ClassLoaderUtil.loadServices(QingZhouApp.class.getName(), loader);
        if (!apps.isEmpty()) {
            appInfo.setQingZhouApp(apps.get(0));
        }
        return appInfo;
    }

    private ModelManagerImpl buildModelManager(File[] appLib, URLClassLoader loader) {
        ModelManagerImpl modelManager = new ModelManagerImpl();
        try {
            modelManager.init(appLib);

            for (String modelName : modelManager.getModelNames()) {
                ModelInfo modelInfo = modelManager.getModelInfo(modelName);
                Class<?> modelClass = loader.loadClass(modelInfo.className);
                if (ModelBase.class.isAssignableFrom(modelClass)) {
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                modelInfo.actionInfoMap.forEach((s, actionInfo) -> {
                    try {
                        Method method = modelClass.getMethod(actionInfo.methodName, Request.class, Response.class);
                        actionInfo.setJavaMethod(method);
                    } catch (Exception e) {
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
    public void installApp(String appName, boolean includeCommon, File... file) throws Exception {
        if (appInfoMap.containsKey(appName)) {
            throw new IllegalArgumentException("The app already exists: " + appName);
        }

        List<File> appLib = new ArrayList<>(Arrays.asList(file));
        if (includeCommon) {
            File[] files = FileUtil.newFile(FrameworkContextImpl.getFrameworkContext().getLib(), "sysapp", "common").listFiles();
            if (files != null) {
                appLib.addAll(Arrays.asList(files));
            }
        }
        AppInfoImpl appInfo = buildAppInfo(appName, appLib.toArray(new File[0]));
        appInfoMap.put(appName, appInfo);

        QingZhouApp qingZhouApp = appInfo.getQingZhouApp();
        if (qingZhouApp instanceof FrameworkContextAware) {
            ((FrameworkContextAware) qingZhouApp).setFrameworkContext(FrameworkContextImpl.getFrameworkContext());
        }
        qingZhouApp.start(appInfo.getAppContext());
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
