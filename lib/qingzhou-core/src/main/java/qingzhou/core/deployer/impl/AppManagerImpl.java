package qingzhou.core.deployer.impl;

import java.io.File;
import java.net.URLClassLoader;
import java.util.*;

import qingzhou.api.*;
import qingzhou.core.deployer.AppManager;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.registry.ModelActionInfo;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;

public class AppManagerImpl implements AppManager {
    private final ModuleContext moduleContext;
    private File appDir;
    private URLClassLoader appLoader;
    private QingzhouApp qingzhouApp;
    private final Set<ActionFilter> appActionFilters = new LinkedHashSet<>();
    private final Map<String, Set<ActionFilter>> modelActionFilters = new LinkedHashMap<>();
    private AuthAdapter authAdapter;
    private AppContextImpl appContext;
    private Properties appProperties;
    private AppInfo appInfo;
    private final Map<String, ModelBase> modelBaseMap = new HashMap<>();
    private final Map<String, Map<String, ActionMethod>> modelActionMap = new HashMap<>();
    private final Map<String, Map<String, ActionMethod>> addedSuperActions = new HashMap<>();
    private final ThreadLocal<Request> threadLocalRequest = new ThreadLocal<>();

    AppManagerImpl(ModuleContext moduleContext) {
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

            Set<ActionFilter> finalFilters = new LinkedHashSet<>(appActionFilters);
            Set<ActionFilter> actionFilters = modelActionFilters.get(request.getModel());
            if (actionFilters != null) {
                finalFilters.addAll(actionFilters);
            }

            for (ActionFilter appActionFilter : finalFilters) {
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

    private void invokeDirectly(Request request) throws Exception {
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

    void setThreadLocalRequest(Request request) {
        if (request == null) return;
        threadLocalRequest.set(request);
    }

    ModuleContext getModuleContext() {
        return moduleContext;
    }

    void invokeSuperAction(Request request) throws Exception {
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

    void addAppActionFilter(ActionFilter... appActionFilter) {
        this.appActionFilters.addAll(Arrays.asList(appActionFilter));
    }
    
    List<ActionFilter> getAppActionFilter() {
        return new ArrayList<>(this.appActionFilters);
    }

    void addModelActionFilter(ModelBase modelBase, ActionFilter... appActionFilter) {
        String modelName = null;
        for (Map.Entry<String, ModelBase> e : modelBaseMap.entrySet()) {
            if (e.getValue().equals(modelBase)) {
                modelName = e.getKey();
            }
        }
        if (modelName == null) throw new IllegalArgumentException();

        this.modelActionFilters.computeIfAbsent(modelName, k -> new LinkedHashSet<>()).addAll(Arrays.asList(appActionFilter));
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

    Properties getAppProperties() {
        return appProperties;
    }

    void setAppProperties(Properties appProperties) {
        this.appProperties = appProperties;
    }
}
