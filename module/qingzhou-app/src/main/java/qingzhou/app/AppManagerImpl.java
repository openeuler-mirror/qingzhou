package qingzhou.app;

import qingzhou.api.App;
import qingzhou.api.ModelBase;
import qingzhou.api.QingzhouApp;
import qingzhou.api.metadata.ModelActionData;
import qingzhou.api.metadata.ModelData;
import qingzhou.api.metadata.ModelFieldData;
import qingzhou.app.bytecode.AnnotationReader;
import qingzhou.app.bytecode.impl.AnnotationReaderImpl;
import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.framework.app.AppInfo;
import qingzhou.framework.app.AppManager;
import qingzhou.framework.app.QingzhouSystemApp;
import qingzhou.framework.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AppManagerImpl implements AppManager {
    private final Map<String, AppInfo> apps = new HashMap<>();
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
        boolean needCommonModel = !AppInfo.SYS_APP_MASTER.equals(appName) && !AppInfo.SYS_APP_NODE_AGENT.equals(appName);
        AppInfoImpl app = buildApp(appName, appFile, needCommonModel);
        apps.put(appName, app);

        QingzhouApp qingzhouApp = app.getQingzhouApp();
        if (qingzhouApp != null) {
            qingzhouApp.start(app.getAppContext());
        }
    }

    @Override
    public void unInstallApp(String appName) throws Exception {
        AppInfoImpl app = (AppInfoImpl) apps.remove(appName);
        if (app != null) {
            try {
                app.getLoader().close();
            } catch (IOException e) {
                Controller.logger.warn("failed to close loader: " + appName, e);
            }

            QingzhouApp qingzhouApp = app.getQingzhouApp();
            if (qingzhouApp != null) {
                File temp = app.getAppContext().getTemp();
                qingzhouApp.stop();
                FileUtil.forceDelete(temp);
            }
        }
    }

    @Override
    public Set<String> getApps() {
        return apps.keySet();
    }

    @Override
    public AppInfo getApp(String name) {
        return apps.get(name);
    }

    private AppInfoImpl buildApp(String appName, File appDir, boolean needCommonModel) throws Exception {
        File[] appFiles = appDir.listFiles();
        if (appFiles == null) {
            throw new IllegalArgumentException("app lib not found: " + appDir.getName());
        }
        File[] appLibs = appFiles;
        if (needCommonModel) {
            File[] commonFiles = FileUtil.newFile(frameworkContext.getLib(), "module", "qingzhou-app", "common").listFiles();
            if (commonFiles != null) {
                int appFileLength = appFiles.length;
                int commonFileLength = commonFiles.length;
                appLibs = new File[appFileLength + commonFileLength];
                System.arraycopy(appFiles, 0, appLibs, 0, appFileLength);
                System.arraycopy(commonFiles, 0, appLibs, appFileLength, commonFileLength);
            }
        }

        AppInfoImpl app = new AppInfoImpl();

        AppContextImpl appContext = new AppContextImpl(frameworkContext);
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
            qingzhouSystemApp.setModuleContext(frameworkContext);
        }

        return app;
    }

    private Class<?> loadAppClass(File[] appLibs, URLClassLoader loader) throws Exception {
        AnnotationReader annotation = AnnotationReaderImpl.getAnnotationReader();
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
                    App app = annotation.readClassAnnotation(cls, App.class);
                    if (app != null) {
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
