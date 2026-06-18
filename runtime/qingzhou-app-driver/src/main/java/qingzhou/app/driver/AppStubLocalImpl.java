package qingzhou.app.driver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.api.*;
import qingzhou.app.driver.systemcall.Icon;
import qingzhou.dto.I18nService;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.registry.AppStubLocal;

class AppStubLocalImpl implements AppStubLocal {
    private final AppContextImpl appContext;
    private final List<ActionFilter> filters;

    private final Map<String, SystemCall> systemCallMap;

    AppStubLocalImpl(AppContextImpl appContext) {
        this.appContext = appContext;

        filters = new ArrayList<>();
        I18nService i18nService = appContext.getService(I18nService.class);
        Validation validation = new Validation(i18nService);
        filters.add(validation);
        filters.addAll(appContext.actionFilters); // 应用拦截器：放在系统拦截器之后，最终 action 之前
        filters.add((request, c) -> invokeAction((RequestImpl) request));

        systemCallMap = new HashMap<String, SystemCall>() {{
            put("icon", new Icon(appContext));
        }};
    }

    @Override
    public AppMeta getAppMeta() {
        return appContext.appMeta;
    }

    @Override
    public AppContext getAppContext() {
        return appContext;
    }

    @Override
    public void invokeApp(RequestImpl request) throws Throwable {
        if (Constants.SYSTEM_MODEL_CODE.equals(request.getModel())) {
            systemCall(request);
            return;
        }

        for (Model m : appContext.appMeta.getApp().models) {
            if (m.code.equals(request.getModel())) {
                for (ModelAction a : m.actions) {
                    if (a.code.equals(request.getAction())) {
                        request.setCurrentModel(m);
                        request.setCurrentModelAction(a);

                        FilterChain chain = new FilterChain() {
                            int index = 0;

                            @Override
                            public void doFilter() throws Throwable {
                                filters.get(index++).doFilter(request, this);
                            }
                        };
                        chain.doFilter();
                        break;
                    }
                }
                break;
            }
        }
    }

    private void invokeAction(RequestImpl request) throws Throwable {
        Model model = request.getCurrentModel();
        ModelAction action = request.getCurrentModelAction();

        ModelBase modelBase = appContext.modelInstances.get(model);
        if (action.isDefaultAction) { // 执行默认 action
            for (Method method : DefaultAction.class.getMethods()) {
                if (method.getName().equals(action.code)) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == 2) {
                        if (QingzhouModel.class.isAssignableFrom(parameterTypes[0])
                                && parameterTypes[1] == Request.class) {
                            invokeMethod(request, method, null, modelBase, request);
                        }
                    }
                    break;
                }
            }
        } else { // 执行自定义 action
            Method method = appContext.actionMethods.get(AppDriver.resolveActionKey(model, action));
            if (method != null) {
                modelBase.setCurrentRequest(request);
                try {
                    invokeMethod(request, method, modelBase, request);
                } finally {
                    modelBase.setCurrentRequest(null);
                }
            }
        }
    }

    private void invokeMethod(RequestImpl request, Method method, Object obj, Object... args) throws Throwable {
        ResponseImpl response = request.getResponse();
        response.setActionInvoked(true);
        try {
            method.invoke(obj, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    // 轻舟系统内部通信，不进入 模块
    private void systemCall(RequestImpl request) throws Exception {
        SystemCall systemCall = systemCallMap.get(request.getAction());
        if (systemCall == null) return;
        else request.getResponse().setActionInvoked(true);

        systemCall.call(request);
    }
}