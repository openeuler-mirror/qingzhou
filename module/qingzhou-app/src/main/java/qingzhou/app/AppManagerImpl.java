package qingzhou.app;

import qingzhou.api.ModelBase;
import qingzhou.api.QingZhouApp;
import qingzhou.api.metadata.ModelActionData;
import qingzhou.api.metadata.ModelData;
import qingzhou.api.metadata.ModelFieldData;
import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.framework.app.App;
import qingzhou.framework.app.AppManager;
import qingzhou.framework.app.QingZhouSystemApp;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class AppManagerImpl implements AppManager {
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
        boolean needCommonApp = !App.SYS_APP_MASTER.equals(appName) && !App.SYS_APP_NODE_AGENT.equals(appName);
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
            throw new IllegalArgumentException("app lib not found: " + appDir.getName());
        }
        File[] appLibs = appFiles;
        if (needCommonApp) {
            File[] commonFiles = FileUtil.newFile(frameworkContext.getLib(), "module", "qingzhou-app", "common").listFiles();
            if (commonFiles != null) {
                int appFileLength = appFiles.length;
                int commonFileLength = commonFiles.length;
                appLibs = new File[appFileLength + commonFileLength];
                System.arraycopy(appFiles, 0, appLibs, 0, appFileLength);
                System.arraycopy(commonFiles, 0, appLibs, appFileLength, commonFileLength);
            }
        }

        AppImpl app = new AppImpl();

        AppContextImpl appContext = new AppContextImpl(frameworkContext);
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
        URLClassLoader loader = new URLClassLoader(urls, needCommonApp ? QingZhouApp.class.getClassLoader() : QingZhouSystemApp.class.getClassLoader());
        app.setLoader(loader);

        ModelManagerImpl modelManager = new ModelManagerImpl();
        modelManager.initModelManager(appLibs, loader, appContext);
        metadata.setModelManager(modelManager);
        initI18n(modelManager, metadata);
        appContext.addActionFilter(new UniqueFilter());

        for (String modelName : modelManager.getModelNames()) {
            ModelBase modelInstance = app.getModelInstance(modelName);
            modelInstance.setAppContext(appContext);
            modelInstance.init();
        }
        try (InputStream inputStream = loader.getResourceAsStream("qingzhou.properties")) {
            Properties properties = FileUtil.streamToProperties(inputStream);
            metadata.setConfig(Collections.unmodifiableMap(new HashMap<String, String>() {{
                properties.forEach((o, o2) -> put((String) o, (String) o2));
            }}));
            String appClass = metadata.getConfig().get("qingzhou.app");
            if (StringUtil.notBlank(appClass)) {
                QingZhouApp qingZhouApp = (QingZhouApp) loader.loadClass(appClass).newInstance();
                app.setQingZhouApp(qingZhouApp);
                if (qingZhouApp instanceof QingZhouSystemApp) {
                    QingZhouSystemApp qingZhouSystemApp = (QingZhouSystemApp) qingZhouApp;
                    qingZhouSystemApp.setModuleContext(frameworkContext);
                }
            }
        }

        return app;
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
