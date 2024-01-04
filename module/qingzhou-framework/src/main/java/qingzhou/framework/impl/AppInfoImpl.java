package qingzhou.framework.impl;

import qingzhou.framework.AppInfo;
import qingzhou.framework.api.ActionFilter;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.QingZhouApp;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.impl.model.ActionInfo;
import qingzhou.framework.impl.model.ModelInfo;
import qingzhou.framework.impl.model.ModelManagerImpl;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppInfoImpl implements AppInfo {
    private QingZhouApp qingZhouApp;
    private AppContextImpl appContext;
    private URLClassLoader loader;

    @Override
    public AppContext getAppContext() {
        return appContext;
    }

    @Override
    public void invokeAction(Request request, Response response) throws Exception {
        ModelManagerImpl modelManager = (ModelManagerImpl) appContext.getModelManager();
        boolean ok = Validator.validate(request, response, modelManager);
        if (!ok) return;

        ModelInfo modelInfo = modelManager.getModelInfo(request.getModelName());
        if (modelInfo == null) return;

        ActionInfo actionInfo = modelInfo.actionInfoMap.get(request.getActionName());
        if (actionInfo == null) return;

        List<Object> args = new ArrayList<>();
        for (Map.Entry<Integer, Class<?>> entry : actionInfo.parameterTypesIndex.entrySet()) {
            Object value;
            if (entry.getValue().isInstance(request)) {
                value = request;
            } else if (entry.getValue().isInstance(response)) {
                value = response;
            } else {
                throw new IllegalStateException();
            }

            args.add(entry.getKey(), value);
        }

        ModelBase modelInstance = modelManager.getModelInstance(request.getModelName());
        modelInstance.setAppContext(appContext);
        List<ActionFilter> actionFilters = appContext.getActionFilters();
        if (actionFilters != null) {
            for (ActionFilter actionFilter : actionFilters) {
                actionFilter.doFilter(request, response);
            }
        }

        actionInfo.javaMethod.invoke(modelInstance, args.toArray());
    }

    public void setAppContext(AppContextImpl appContext) {
        this.appContext = appContext;
    }

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
