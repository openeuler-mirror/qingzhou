package qingzhou.framework.impl;

import qingzhou.framework.AppInfo;
import qingzhou.framework.AppManager;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.api.QingZhouApp;
import qingzhou.framework.impl.model.ModelManagerImpl;
import qingzhou.framework.util.ClassLoaderUtil;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.ServerUtil;

import java.io.File;
import java.net.URLClassLoader;
import java.util.*;

public class AppManagerImpl implements AppManager {
    private final Map<String, AppInfo> appInfoMap = new HashMap<>();

    private AppInfoImpl buildAppInfo(URLClassLoader loader) {
        AppInfoImpl appInfo = new AppInfoImpl();
        AppContextImpl appContext = new AppContextImpl((FrameworkContextImpl) ServerUtil.getFrameworkContext());
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
    public void installApp(String appName, boolean includeCommon, File... file) throws Exception {
        if (appInfoMap.containsKey(appName)) {
            throw new IllegalArgumentException("The app already exists: " + appName);
        }

        List<File> appLib = new ArrayList<>(Arrays.asList(file));
        if (includeCommon) {
            File[] files = FileUtil.newFile(ServerUtil.getFrameworkContext().getLib(), "sysapp", "common").listFiles();
            if (files != null) {
                appLib.addAll(Arrays.asList(files));
            }
        }
        URLClassLoader loader = ClassLoaderUtil.newURLClassLoader(appLib, QingZhouApp.class.getClassLoader());
        AppInfoImpl appInfo = buildAppInfo(loader);
        appInfoMap.put(appName, appInfo);

        appInfo.getQingZhouApp().start(appInfo.getAppContext());
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
