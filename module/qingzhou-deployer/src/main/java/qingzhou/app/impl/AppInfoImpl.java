package qingzhou.app.impl;

import qingzhou.app.impl.model.ActionInfo;
import qingzhou.app.impl.model.ModelInfo;
import qingzhou.framework.AppInfo;
import qingzhou.framework.api.*;

import java.net.URLClassLoader;
import java.util.List;
import java.util.Properties;

public class AppInfoImpl implements AppInfo {
    private QingZhouApp qingZhouApp;
    private AppContextImpl appContext;
    private URLClassLoader loader;
    private Properties appProperties;

    @Override
    public Properties getAppProperties() {
        return appProperties;
    }

    @Override
    public AppContext getAppContext() {
        return appContext;
    }

    @Override
    public void invokeAction(Request request, Response response) throws Exception {
        ModelManagerImpl modelManager = (ModelManagerImpl) appContext.getConsoleContext().getModelManager();

        ModelInfo modelInfo = modelManager.getModelInfo(request.getModelName());
        if (modelInfo == null) return;

        ActionInfo actionInfo = modelInfo.actionInfoMap.get(request.getActionName());
        if (actionInfo == null) return;

        ModelBase modelInstance = modelManager.getModelInstance(request.getModelName());
        modelInstance.setAppContext(appContext);
        List<ActionFilter> actionFilters = appContext.getActionFilters();
        if (actionFilters != null) {
            for (ActionFilter actionFilter : actionFilters) {
                if (!actionFilter.doFilter(request, response)) {
                    response.setSuccess(false);
                    return;
                }
            }
        }

        actionInfo.getJavaMethod().invoke(modelInstance, request, response);
    }

    public void setAppContext(AppContextImpl appContext) {
        this.appContext = appContext;
    }

    @Override
    public QingZhouApp getQingZhouApp() {
        return qingZhouApp;
    }

    public void setQingZhouApp(QingZhouApp qingZhouApp) {
        this.qingZhouApp = qingZhouApp;
    }

    public URLClassLoader getLoader() {
        return loader;
    }

    public void setLoader(URLClassLoader loader) {
        this.loader = loader;
    }

    public void setAppProperties(Properties appProperties) {
        this.appProperties = appProperties;
    }
}
