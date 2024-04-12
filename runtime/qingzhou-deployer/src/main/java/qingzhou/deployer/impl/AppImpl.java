package qingzhou.deployer.impl;

import qingzhou.api.*;
import qingzhou.deployer.App;
import qingzhou.console.ResponseImpl;

import java.net.URLClassLoader;

public class AppImpl implements App {
    private QingzhouApp qingzhouApp;
    private AppContextImpl appContext;
    private URLClassLoader loader;
    private Validator validator;

    @Override
    public AppContextImpl getAppContext() {
        return appContext;
    }

    @Override
    public void invoke(Request request, Response response) throws Exception {
        String modelName = request.getModelName();
        ModelManagerImpl modelManager = (ModelManagerImpl) appContext.getAppMetadata().getModelManager();
        ModelInfo modelInfo = modelManager.getModelInfo(modelName);
        if (modelInfo == null) return;

        String actionName = request.getActionName();
        ActionInfo actionInfo = modelInfo.actionInfoMap.get(actionName);
        if (actionInfo == null) return;

        ResponseImpl validationResponse = new ResponseImpl();
        if (validator == null) {
            validator = new Validator(appContext);
        }
        boolean ok = validator.validate(request, validationResponse);// 本地和远程走这统一的一次校验
        if (!ok) {
            if (validationResponse.getMsg() == null) {
                validationResponse.setMsg(appContext.getAppMetadata().getI18n(request.getI18nLang(), "validator.fail"));
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

    @Override
    public ModelBase getModelInstance(String modelName) {
        return ((ModelManagerImpl) appContext.getAppMetadata().getModelManager()).getModelInfo(modelName).instance;
    }

    public void setAppContext(AppContextImpl appContext) {
        this.appContext = appContext;
    }

    @Override
    public QingzhouApp getQingzhouApp() {
        return qingzhouApp;
    }

    public void setQingzhouApp(QingzhouApp qingzhouApp) {
        this.qingzhouApp = qingzhouApp;
    }

    public URLClassLoader getLoader() {
        return loader;
    }

    public void setLoader(URLClassLoader loader) {
        this.loader = loader;
    }
}
