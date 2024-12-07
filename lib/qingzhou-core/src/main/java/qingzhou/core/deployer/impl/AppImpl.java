package qingzhou.core.deployer.impl;

import qingzhou.api.ActionFilter;
import qingzhou.api.ModelBase;
import qingzhou.api.QingzhouApp;
import qingzhou.api.Request;
import qingzhou.core.deployer.App;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.registry.ModelActionInfo;
import qingzhou.engine.util.Utils;

import java.net.URLClassLoader;
import java.util.*;

class AppImpl implements App {
    private Properties appProperties;
    private URLClassLoader loader;
    private AppInfo appInfo;
    private AppContextImpl appContext;
    private QingzhouApp qingzhouApp;
    private final Map<String, ModelBase> modelBaseMap = new HashMap<>();
    private final Map<String, Map<String, ActionMethod>> modelActionMap = new HashMap<>();
    private final Map<String, Map<String, ActionMethod>> addedSuperActions = new HashMap<>();

    @Override
    public AppContextImpl getAppContext() {
        return appContext;
    }

    @Override
    public AppInfo getAppInfo() {
        return appInfo;
    }

    @Override
    public void invoke(Request request) throws Throwable {
        Utils.doInThreadContextClassLoader(getLoader(), () -> {
            for (ActionFilter actionFilter : appContext.getActionFilters()) {
                String msg = actionFilter.doFilter(request);
                if (msg != null) {
                    request.getResponse().setSuccess(false);
                    request.getResponse().setMsg(msg);
                }
            }

            invokeDirectly(request);
        });
    }

    void invokeDirectly(Request request) throws Exception {
        String modelName = request.getModel();
        Map<String, ActionMethod> methodMap = modelActionMap.get(modelName);
        if (methodMap == null) return;

        String actionName = request.getAction();
        ActionMethod actionMethod = methodMap.get(actionName);
        if (actionMethod == null) return;
        getAppContext().setCurrentRequest(request);
        actionMethod.invoke(request);
    }

    void invokeDefault(Request request) throws Exception {
        String modelName = request.getModel();
        String actionName = request.getAction();

        Map<String, ActionMethod> actionMethodMap = addedSuperActions.computeIfAbsent(modelName, k -> new HashMap<>());
        ActionMethod actionMethod = actionMethodMap.computeIfAbsent(actionName, s -> {
            ModelBase modelBase = modelBaseMap.get(modelName);
            Class<? extends ModelBase> modelClass = modelBase.getClass();
            Set<String> superActions = new HashSet<>();
            DeployerImpl.findSuperActions(modelClass, superActions);
            if (superActions.contains(actionName)) {
                for (ModelActionInfo actionInfo : SuperAction.ALL_SUPER_ACTION_CACHE) {
                    if (actionInfo.getCode().equals(actionName)) {
                        return ActionMethod.buildActionMethod(actionInfo.getMethod(), new SuperAction(this, modelBase));
                    }
                }
            }

            return null;
        });

        if (actionMethod != null) {
            actionMethod.invoke(request);
            return;
        }

        throw new IllegalArgumentException("The default action was not found for model: " + modelName + ", action: " + actionName);
    }

    Map<String, Map<String, ActionMethod>> getModelActionMap() {
        return modelActionMap;
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

    void setAppInfo(AppInfo appInfo) {
        this.appInfo = appInfo;
    }

    Map<String, ModelBase> getModelBaseMap() {
        return modelBaseMap;
    }

    public Properties getAppProperties() {
        return appProperties;
    }

    public void setAppProperties(Properties appProperties) {
        this.appProperties = appProperties;
    }
}
