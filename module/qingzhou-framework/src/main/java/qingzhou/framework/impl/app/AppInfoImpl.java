package qingzhou.framework.impl.app;

import qingzhou.api.AppContext;
import qingzhou.api.AppContextHelper;
import qingzhou.api.QingZhouApp;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.framework.AppInfo;
import qingzhou.framework.impl.FrameworkContextImpl;
import qingzhou.framework.impl.app.model.ActionInfo;
import qingzhou.framework.impl.app.model.ModelInfo;
import qingzhou.framework.impl.app.model.ModelManagerImpl;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppInfoImpl implements AppInfo {
    private String name;
    private QingZhouApp qingZhouApp;
    private AppContextImpl appContext;
    private URLClassLoader classLoader;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public QingZhouApp getQingZhouApp() {
        return qingZhouApp;
    }

    public void setQingZhouApp(QingZhouApp qingZhouApp) {
        this.qingZhouApp = qingZhouApp;
    }

    @Override
    public AppContextImpl getAppContext() {
        return appContext;
    }

    @Override
    public void invokeAction(Request request, Response response) throws Exception {
        ModelManagerImpl modelManager = (ModelManagerImpl) appContext.getConsoleContext().getModelManager();
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

        AppInfo appInfo = FrameworkContextImpl.getInstance().getAppInfoManager().getAppInfo(request.getAppName());
        AppContext appContext = appInfo.getAppContext();
        AppContextHelper.setAppContext(appContext);// todo 后续删除

        javaMethod.invoke(modelManager.getModelInstance(request.getModelName()), args.toArray());
        filterResponse(request, response, modelManager);// todo 后续 剥离掉 LIstModel api 里面的逻辑后，这块就挪动到 listIntern 内部实现的尾部。
    }


    private void filterResponse(Request request, Response response, ModelManagerImpl modelManager) {
        String[] showField = modelManager.getShowField(request.getModelName(), request.getActionName());
        if (showField != null) {
            List<Map<String, String>> dataList = response.getDataList();
            for (Map<String, String> map : dataList) {
                for (String field : showField) {
                    if (!map.containsKey(field)) {
                        map.remove(field);
                    }
                }
            }
        }
    }

    public void setAppContext(AppContextImpl appContext) {
        this.appContext = appContext;
    }

    public URLClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(URLClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
