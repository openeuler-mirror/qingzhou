package qingzhou.core.deployer.impl;

import qingzhou.api.*;
import qingzhou.core.deployer.App;
import qingzhou.core.deployer.I18nTool;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.registry.ModelActionInfo;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;

import java.io.File;
import java.net.URLClassLoader;
import java.util.*;

public class AppImpl implements App {
    private final ModuleContext moduleContext;
    private File appDir;
    private URLClassLoader appLoader;
    private QingzhouApp qingzhouApp;
    private ActionFilter appActionFilter;
    private AuthAdapter authAdapter;
    private Properties appProperties;
    private AppContextImpl appContext;
    private AppInfo appInfo;
    private final Map<String, ModelBase> modelBaseMap = new HashMap<>();
    private final Map<String, Map<String, ActionMethod>> modelActionMap = new HashMap<>();
    private final Map<String, Map<String, ActionMethod>> addedSuperActions = new HashMap<>();
    private File appTemp;
    private final I18nTool i18nTool = new I18nTool();
    private final ThreadLocal<Request> threadLocalRequest = new ThreadLocal<>();

    AppImpl(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

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
        Utils.doInThreadContextClassLoader(getAppLoader(), () -> {
            if (appActionFilter != null) {
                String msg = appActionFilter.doFilter(request);
                if (msg != null) {
                    request.getResponse().setSuccess(false);
                    request.getResponse().setMsg(msg);
                    return;
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

        threadLocalRequest.set(request);
        try {
            actionMethod.invoke(request);
        } finally {
            threadLocalRequest.remove();
        }
    }

    Request getThreadLocalRequest() {
        return threadLocalRequest.get();
    }

    ModuleContext getModuleContext() {
        return moduleContext;
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

    QingzhouApp getQingzhouApp() {
        return qingzhouApp;
    }

    void setQingzhouApp(QingzhouApp qingzhouApp) {
        this.qingzhouApp = qingzhouApp;
    }

    URLClassLoader getAppLoader() {
        return appLoader;
    }

    void setAppLoader(URLClassLoader appLoader) {
        this.appLoader = appLoader;
    }

    void setAppInfo(AppInfo appInfo) {
        this.appInfo = appInfo;
    }

    Map<String, ModelBase> getModelBaseMap() {
        return modelBaseMap;
    }

    Properties getAppProperties() {
        return appProperties;
    }

    void setAppProperties(Properties appProperties) {
        this.appProperties = appProperties;
    }

    void setAppActionFilter(ActionFilter appActionFilter) {
        this.appActionFilter = appActionFilter;
    }

    void setAuthAdapter(AuthAdapter authAdapter) {
        this.authAdapter = authAdapter;
    }

    public AuthAdapter getAuthAdapter() {
        return authAdapter;
    }

    File getAppDir() {
        return appDir;
    }

    void setAppDir(File appDir) {
        this.appDir = appDir;
    }

    File getAppTemp() {
        if (appTemp == null) {
            appTemp = new File(moduleContext.getTemp(), appInfo.getName());
        }
        return appTemp;
    }

    I18nTool getI18nTool() {
        return i18nTool;
    }
}
