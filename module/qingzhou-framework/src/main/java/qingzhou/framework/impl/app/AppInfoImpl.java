package qingzhou.framework.impl.app;

import qingzhou.api.QingZhouApp;
import qingzhou.framework.AppInfo;

import java.net.URLClassLoader;

public class AppInfoImpl implements AppInfo {
    private String name;
    private QingZhouApp qingZhouApp;
    private AppContextImpl appContext;
    private URLClassLoader classLoader;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public QingZhouApp getQingZhouApp() {
        return qingZhouApp;
    }

    public void setQingZhouApp(QingZhouApp qingZhouApp) {
        this.qingZhouApp = qingZhouApp;
    }

    @Override
    public AppContextImpl getAppContext() {
        return appContext;
    }

    public void setAppContext(AppContextImpl appContext) {
        this.appContext = appContext;
    }

    public URLClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(URLClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
