package qingzhou.app;

import qingzhou.api.*;
import qingzhou.framework.app.App;

import java.lang.reflect.Method;
import java.net.URLClassLoader;

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
        ModelManagerImpl modelManager = (ModelManagerImpl) appContext.getAppMetadata().getModelManager();
        ModelInfo modelInfo = modelManager.getModelInfo(modelName);
        if (modelInfo == null) return;

        String actionName = request.getActionName();
        ActionInfo actionInfo = modelInfo.actionInfoMap.get(actionName);
        if (actionInfo == null) return;

        for (ActionFilter actionFilter : appContext.getActionFilters()) {
            String msg = actionFilter.doFilter(request, response, appContext);
            if (msg != null) {
                response.setSuccess(false);
                response.setMsg(msg);
                return;
            }
        }

        actionInfo.invokeMethod.invoke(request, response);
    }

    @Override
    public ModelBase getModelInstance(String modelName) {
        return ((ModelManagerImpl) appContext.getAppMetadata().getModelManager()).getModelInfo(modelName).getInstance();
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
