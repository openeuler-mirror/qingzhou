package qingzhou.app;

import qingzhou.api.*;
import qingzhou.framework.app.App;

import java.net.URLClassLoader;
import java.util.List;

public class AppImpl implements App {
    private QingZhouApp qingZhouApp;
    private AppContextImpl appContext;
    private URLClassLoader loader;

    @Override
    public AppContextImpl getAppContext() {
        return appContext;
    }

    @Override
    public void invoke(Request request, Response response) throws Exception {
        String modelName = request.getModelName();
        String actionName = request.getActionName();

        ModelManagerImpl modelManager = (ModelManagerImpl) appContext.getAppMetadata().getModelManager();

        ModelInfo modelInfo = modelManager.getModelInfo(modelName);
        if (modelInfo == null) return;

        ActionInfo actionInfo = modelInfo.actionInfoMap.get(actionName);
        if (actionInfo == null) return;

        ModelBase modelInstance = getModelInstance(modelName);
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

    @Override
    public ModelBase getModelInstance(String modelName) {
        return ((ModelManagerImpl) getAppContext().getAppMetadata().getModelManager()).getModelInfo(modelName).getInstance();
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
}