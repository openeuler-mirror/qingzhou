package qingzhou.app.driver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
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
import qingzhou.registry.Constants;

class AppStubLocalImpl implements AppStubLocal {
    private final AppContextImpl appContext;
    private final List<ActionFilter> systemFilters;
    // 持有实时列表而非构造期快照：应用在启动之后新增的拦截器不应静默失效
    private final List<ActionFilter> appFilters;

    private final Map<String, SystemCall> systemCallMap;

    // 默认 action 方法清单在类加载时反射一次并缓存，避免每次请求都扫描 DefaultAction 的全部方法
    private static final Method[] DEFAULT_ACTION_METHODS = DefaultAction.class.getMethods();

    AppStubLocalImpl(AppContextImpl appContext) {
        this.appContext = appContext;

        I18nService i18nService = appContext.getService(I18nService.class);
        systemFilters = Collections.singletonList(new Validation(i18nService));
        appFilters = appContext.actionFilters; // 应用拦截器：排在系统拦截器之后、最终 action 之前

        systemCallMap = Collections.singletonMap("icon", new Icon(appContext, appContext.appMeta.getApp().icon));
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
                                int next = index++;
                                if (next < systemFilters.size()) {
                                    systemFilters.get(next).doFilter(request, this);
                                } else if (next - systemFilters.size() < appFilters.size()) {
                                    appFilters.get(next - systemFilters.size()).doFilter(request, this);
                                } else {
                                    invokeAction(request);
                                }
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
            for (Method method : DEFAULT_ACTION_METHODS) {
                // 默认操作名已由 list 改为 page（action.code），方法名仍为 list，用 methodName 匹配
                if (method.getName().equals(action.methodName)) {
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
                    modelBase.clearCurrentRequest();
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

        request.getResponse().setActionInvoked(true);
        systemCall.call(request);
    }
}