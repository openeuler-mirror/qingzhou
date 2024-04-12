package qingzhou.deployer.impl;

import qingzhou.api.ModelBase;
import qingzhou.api.QingzhouApp;
import qingzhou.api.metadata.ModelActionData;
import qingzhou.api.metadata.ModelData;
import qingzhou.api.metadata.ModelFieldData;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DeployerImpl implements Deployer {
    private final Map<String, App> apps = new HashMap<>();
    private final ModuleContext moduleContext;

    public DeployerImpl(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void installApp(File appFile) throws Exception {
        String appName = appFile.getName();
        if (apps.containsKey(appName)) {
            throw new IllegalArgumentException("The app already exists: " + appName);
        }
        boolean needCommonModel = !App.SYS_APP_MASTER.equals(appName) && !App.SYS_APP_NODE_AGENT.equals(appName);
        AppImpl app = buildApp(appName, appFile, needCommonModel);
        apps.put(appName, app);

        QingzhouApp qingzhouApp = app.getQingzhouApp();
        if (qingzhouApp != null) {
            qingzhouApp.start(app.getAppContext());
        }
    }

    @Override
    public void unInstallApp(String appName) throws Exception {
        AppImpl app = (AppImpl) apps.remove(appName);
        if (app == null) return;

        File temp = null;

        QingzhouApp qingzhouApp = app.getQingzhouApp();
        if (qingzhouApp != null) {
            temp = app.getAppContext().getTemp();
            qingzhouApp.stop();
        }

        try {
            app.getLoader().close();
        } catch (IOException e) {
            Controller.logger.warn("failed to close loader: " + appName, e);
        }

        if (temp != null) {
            FileUtil.forceDelete(temp);
        }
    }

    @Override
    public Collection<String> getAllApp() {
        return apps.keySet();
    }

    @Override
    public App getApp(String name) {
        return apps.get(name);
    }

    private AppImpl buildApp(String appName, File appDir, boolean needCommonModel) throws Exception {
        File[] appFiles = appDir.listFiles();
        if (appFiles == null) {
            throw new IllegalArgumentException("app lib not found: " + appDir.getName());
        }
        File[] appLibs = appFiles;
        if (needCommonModel) {
            File[] commonFiles = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", "common").listFiles();
            if (commonFiles != null) {
                int appFileLength = appFiles.length;
                int commonFileLength = commonFiles.length;
                appLibs = new File[appFileLength + commonFileLength];
                System.arraycopy(appFiles, 0, appLibs, 0, appFileLength);
                System.arraycopy(commonFiles, 0, appLibs, appFileLength, commonFileLength);
            }
        }

        AppImpl app = new AppImpl();

        AppContextImpl appContext = new AppContextImpl(moduleContext);
        appContext.addI18n("validator.fail", new String[]{"部分数据不合法", "en:Some of the data is not legitimate"});
        app.setAppContext(appContext);
        AppMetadataImpl metadata = (AppMetadataImpl) appContext.getAppMetadata();
        metadata.setAppName(appName);

        URL[] urls = Arrays.stream(appLibs).map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toArray(URL[]::new);
        URLClassLoader loader = new URLClassLoader(urls, QingzhouSystemApp.class.getClassLoader());
        app.setLoader(loader);

        ModelManagerImpl modelManager = new ModelManagerImpl();
        modelManager.initModelManager(appLibs, loader);
        metadata.setModelManager(modelManager);
        initI18n(modelManager, metadata);
        appContext.addActionFilter(new UniqueFilter());

        for (String modelName : modelManager.getModelNames()) {
            ModelBase modelInstance = app.getModelInstance(modelName);
            modelInstance.setAppContext(appContext);
            modelInstance.init();
        }

        Class<?> appClass = loadAppClass(appLibs, loader);
        QingzhouApp qingzhouApp = (QingzhouApp) appClass.newInstance();
        app.setQingzhouApp(qingzhouApp);
        if (qingzhouApp instanceof QingzhouSystemApp) {
            QingzhouSystemApp qingzhouSystemApp = (QingzhouSystemApp) qingzhouApp;
            qingzhouSystemApp.setModuleContext(moduleContext);
        }

        return app;
    }

    private Class<?> loadAppClass(File[] appLibs, URLClassLoader loader) throws Exception {
        for (File file : appLibs) {
            if (!file.getName().endsWith(".jar")) continue;
            try (JarFile jar = new JarFile(file)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String entryName = entries.nextElement().getName();
                    if (entryName.contains("$") || !entryName.endsWith(".class")) continue;
                    int i = entryName.indexOf(".class");
                    String className = entryName.substring(0, i).replace("/", ".");
                    Class<?> cls = loader.loadClass(className);
                    if (cls.getDeclaredAnnotation(qingzhou.api.App.class) != null) {
                        return cls;
                    }
                }
            }
        }

        throw new IllegalStateException("The main class of the app is missing");
    }


    private void initI18n(ModelManagerImpl modelManager, AppMetadataImpl metadata) {
        for (String modelName : modelManager.getModelNames()) {
            final ModelData model = modelManager.getModel(modelName);

            // for i18n
            metadata.addI18n("model." + modelName, model.nameI18n());
            metadata.addI18n("model.info." + modelName, model.infoI18n());

            Arrays.stream(modelManager.getFieldNames(modelName)).forEach(k -> {
                ModelFieldData v = modelManager.getModelField(modelName, k);
                metadata.addI18n("model.field." + modelName + "." + k, v.nameI18n());
                String[] info = v.infoI18n();
                if (info.length > 0) {
                    metadata.addI18n("model.field.info." + modelName + "." + k, info);
                }
            });

            for (String actionName : modelManager.getActionNames(modelName)) {
                ModelActionData modelAction = modelManager.getModelAction(modelName, actionName);
                if (modelAction != null) {// todo  disable 后 有 null 的情况?
                    metadata.addI18n("model.action." + modelName + "." + modelAction.name(), modelAction.nameI18n());
                    metadata.addI18n("model.action.info." + modelName + "." + modelAction.name(), modelAction.infoI18n());
                }
            }
        }
    }
}
