package qingzhou.app.impl;

import qingzhou.framework.app.App;
import qingzhou.framework.api.ActionFilter;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.QingZhouApp;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;

import java.net.URLClassLoader;
import java.util.List;
import java.util.Properties;

public class AppImpl implements App {
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
    public void invoke(Request request, Response response) throws Exception {
        String modelName = request.getModelName();
        String actionName = request.getActionName();

        ModelManagerImpl modelManager = (ModelManagerImpl) appContext.getConsoleContext().getModelManager();

        ModelInfo modelInfo = modelManager.getModelInfo(modelName);
        if (modelInfo == null) return;

        ActionInfo actionInfo = modelInfo.actionInfoMap.get(actionName);
        if (actionInfo == null) return;

        ModelBase modelInstance = modelManager.getModelInstance(modelName);
        modelInstance.setAppContext(appContext);
        modelInstance.setI18nLang(request.getI18nLang());
        List<ActionFilter> actionFilters = appContext.getActionFilters();
        if (actionFilters != null) {
            for (ActionFilter actionFilter : actionFilters) {
                String msg = actionFilter.doFilter(request, response, appContext);
                if (msg != null) {
                    response.setMsg(msg);
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
