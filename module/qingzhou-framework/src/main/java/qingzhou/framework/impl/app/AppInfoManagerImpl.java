package qingzhou.framework.impl.app;

import qingzhou.api.Constants;
import qingzhou.api.QingZhouApp;
import qingzhou.framework.AppInfo;
import qingzhou.framework.AppInfoManager;
import qingzhou.framework.impl.ServerUtil;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppInfoManagerImpl implements AppInfoManager {
    private AppInfoImpl masterAppInfo;
    private final Map<String, AppInfoImpl> appInfoMap = new HashMap<>();

    @Override
    public void addMasterApp(AppInfoImpl appInfo) {
        if (masterAppInfo == null) {
            masterAppInfo = appInfo;
        }
    }

    @Override
    public boolean addApp(File appFile) {
        AppInfoImpl appInfo = buildAppInfo(appFile);
        if (appInfo == null) return false;
        String name = appInfo.getName();
        appInfoMap.put(name, appInfo);
        return true;
    }

    @Override
    public boolean removeApp(String name) {
        AppInfoImpl appInfo = appInfoMap.remove(name);
        try {
            appInfo.getClassLoader().close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Set<String> getApps() {
        return Collections.unmodifiableSet(appInfoMap.keySet());
    }

    @Override
    public AppInfo getAppInfo(String name) {
        if (name == null || name.equals(Constants.QINGZHOU_MASTER_APP_NAME)) return masterAppInfo;

        return appInfoMap.get(name);
    }

    private AppInfoImpl buildAppInfo(File appFile) {
        List<File> appLib = buildAppLib(appFile);
        if (appLib == null) return null;

        URLClassLoader loader = ServerUtil.newURLClassLoader(appLib.toArray(new File[0]), QingZhouApp.class.getClassLoader());
        List<QingZhouApp> appsList = ServerUtil.loadServices(QingZhouApp.class.getName(), loader);

        AppInfoImpl appInfo = new AppInfoImpl();
        appInfo.setClassLoader(loader);
        appInfo.setQingZhouApp(appsList.get(0));
        String appName = appFile.getName();
        appInfo.setName(appName);
        return appInfo;
    }

    private List<File> buildAppLib(File appDir) {
        if (appDir == null) return null;
        File appLib = new File(appDir, "lib");
        if (!appLib.isDirectory() || appLib.list() == null) {
            return null;
        }

        List<File> appLibs = new ArrayList<>();
        findAppLib(appLib, appLibs); // 应用的jar

        File app = new File(ServerUtil.getLib(), "app");
        findAppLib(app, appLibs); // 内置的应用jar
        return appLibs;
    }

    private void findAppLib(File findInFile, List<File> addToLibs) {
        if (findInFile.isDirectory()) {
            File[] files = findInFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    findAppLib(file, addToLibs);
                }
            }
        } else {
            if (findInFile.getName().endsWith(".jar")) {
                addToLibs.add(findInFile);
            }
        }
    }
}
