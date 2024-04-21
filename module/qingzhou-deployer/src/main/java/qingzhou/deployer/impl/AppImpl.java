package qingzhou.deployer.impl;

import qingzhou.api.ActionFilter;
import qingzhou.api.QingzhouApp;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.deployer.App;
import qingzhou.registry.AppInfo;

import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

class AppImpl implements App {
    private URLClassLoader loader;
    private AppInfo appInfo;
    private AppContextImpl appContext;
    private QingzhouApp qingzhouApp;

    private final Map<String, Map<String, ActionMethod>> actionMap = new HashMap<>();

    @Override
    public AppInfo getAppInfo() {
        return appInfo;
    }

    void setAppInfo(AppInfo appInfo) {
        this.appInfo = appInfo;
    }

    @Override
    public AppContextImpl getAppContext() {
        return appContext;
    }

    @Override
    public void invoke(Request request, Response response) throws Exception {
        for (ActionFilter actionFilter : appContext.getActionFilters()) {
            String msg = actionFilter.doFilter(request, response);
            if (msg != null) {
                response.setSuccess(false);
                response.setMsg(msg);
                return;
            }
        }

        invokeDirectly(request, response);
    }

    @Override
    public void invokeDirectly(Request request, Response response) throws Exception {
        String modelName = request.getModel();
        Map<String, ActionMethod> methodMap = actionMap.get(modelName);
        if (methodMap == null) return;

        String actionName = request.getAction();
        ActionMethod actionMethod = methodMap.get(actionName);
        if (actionMethod == null) return;

        actionMethod.invoke(request, response);
    }

    Map<String, Map<String, ActionMethod>> getActionMap() {
        return actionMap;
    }

    void setAppContext(AppContextImpl appContext) {
        this.appContext = appContext;
    }

    @Override
    public QingzhouApp getQingzhouApp() {
        return qingzhouApp;
    }

    void setQingzhouApp(QingzhouApp qingzhouApp) {
        this.qingzhouApp = qingzhouApp;
    }

    URLClassLoader getLoader() {
        return loader;
    }

    void setLoader(URLClassLoader loader) {
        this.loader = loader;
    }
}
