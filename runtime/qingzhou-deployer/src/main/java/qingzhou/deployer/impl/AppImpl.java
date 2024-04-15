package qingzhou.deployer.impl;

import qingzhou.api.*;
import qingzhou.deployer.ResponseImpl;
import qingzhou.deployer.App;
import qingzhou.registry.AppInfo;

import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

class AppImpl implements App {
    private URLClassLoader loader;
    private AppInfo appInfo;
    private AppContextImpl appContext;
    private Map<String, ModelBase> modelBaseMap = new HashMap<>();
    private QingzhouApp qingzhouApp;
    private Validator validator;

    private Map<String, Map<String, ActionMethod>> actionMap = new HashMap<>();

    AppInfo getAppInfo() {
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
        String modelName = request.getModelName();
        Map<String, ActionMethod> methodMap = actionMap.get(modelName);
        if (methodMap == null) return;

        String actionName = request.getActionName();
        ActionMethod actionMethod = methodMap.get(actionName);
        if (actionMethod == null) return;

        ResponseImpl validationResponse = new ResponseImpl();
        if (validator == null) {
            validator = new Validator(appContext);
        }
        boolean ok = validator.validate(request, validationResponse);// 本地和远程走这统一的一次校验
        if (!ok) {
            if (validationResponse.getMsg() == null) {
                validationResponse.setMsg(appContext.getI18n(request.getI18nLang(), "validator.fail"));
            }
            return;
        }

        for (ActionFilter actionFilter : appContext.getActionFilters()) {
            String msg = actionFilter.doFilter(request, response, appContext);
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
        ModelManagerImpl modelManager = (ModelManagerImpl) appContext.getAppMetadata().getModelManager();
        ActionInfo.InvokeMethod invokeMethod = modelManager.getModelInfo(request.getModelName()).actionInfoMap.get(request.getActionName()).invokeMethod;
        invokeMethod.invokeMethod(request, response);
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
