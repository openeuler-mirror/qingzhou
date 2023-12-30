package qingzhou.framework.impl;

import qingzhou.framework.AppInfo;
import qingzhou.framework.AppManager;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.api.QingZhouApp;
import qingzhou.framework.impl.model.ModelManagerImpl;
import qingzhou.framework.util.ClassLoaderUtil;

import java.io.File;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppManagerImpl implements AppManager {
    private final Map<String, AppInfo> appInfoMap = new HashMap<>();
    private final Map<String, URLClassLoader> loaderMap = new HashMap<>();

    private AppInfoImpl buildAppInfo(URLClassLoader loader) {
        AppInfoImpl appInfo = new AppInfoImpl();
        AppContextImpl appContext = new AppContextImpl((FrameworkContextImpl) FrameworkContextImpl.getFrameworkContext());
        ModelManager modelManager = new ModelManagerImpl(loader);
        appContext.setModelManager(modelManager);
        appContext.setConsoleContext(new ConsoleContextImpl(modelManager));
        appInfo.setAppContext(appContext);
        List<QingZhouApp> apps = ClassLoaderUtil.loadServices(QingZhouApp.class.getName(), loader);
        if (!apps.isEmpty()) {
            appInfo.setQingZhouApp(apps.get(0));
        }
        return appInfo;
    }

    @Override
    public void installApp(String appName, File appFile) throws Exception {
        URLClassLoader loader = ClassLoaderUtil.newURLClassLoader(appFile, QingZhouApp.class.getClassLoader());
        installApp(appName, loader);

        loaderMap.put(appName, loader);
    }

    @Override
    public void installApp(String appName, URLClassLoader loader) throws Exception {
        if (appInfoMap.containsKey(appName)) {
            throw new IllegalArgumentException("The app already exists: " + appName);
        }

        AppInfoImpl appInfo = buildAppInfo(loader);
        appInfo.getQingZhouApp().start(appInfo.getAppContext());
        appInfoMap.put(appName, appInfo);
    }

    @Override
    public void uninstallApp(String name) throws Exception {
        AppInfoImpl appInfo = (AppInfoImpl) appInfoMap.remove(name);
        if (appInfo == null) return;

        QingZhouApp qingZhouApp = appInfo.getQingZhouApp();
        if (qingZhouApp != null) {
            qingZhouApp.stop();
        }

        URLClassLoader loader = loaderMap.remove(name);
        if (loader != null) {
            loader.close();
        }
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
