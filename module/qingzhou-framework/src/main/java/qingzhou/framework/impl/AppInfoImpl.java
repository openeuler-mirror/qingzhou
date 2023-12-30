package qingzhou.framework.impl;

import qingzhou.framework.AppInfo;
import qingzhou.framework.api.*;
import qingzhou.framework.impl.model.ActionInfo;
import qingzhou.framework.impl.model.ModelInfo;
import qingzhou.framework.impl.model.ModelManagerImpl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppInfoImpl implements AppInfo {
    private QingZhouApp qingZhouApp;
    private AppContext appContext;

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

        Method javaMethod = actionInfo.javaMethod;

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
        javaMethod.invoke(modelInstance, args.toArray());
    }

    public void setAppContext(AppContext appContext) {
        this.appContext = appContext;
    }

    public QingZhouApp getQingZhouApp() {
        return qingZhouApp;
    }

    public void setQingZhouApp(QingZhouApp qingZhouApp) {
        this.qingZhouApp = qingZhouApp;
    }
}
