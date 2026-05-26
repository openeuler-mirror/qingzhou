package qingzhou.agent.embedded.driver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import qingzhou.agent.embedded.i18n.I18nService;
import qingzhou.api.*;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.registry.AppStubLocal;

class AppStubLocalImpl implements AppStubLocal {
    private final AppContextImpl appContext;
    private final AppMeta appMeta;
    private final I18nService i18nService;
    private final Validation validation;

    private final String[] MSG_DATA_VALIDATION_FAILED = {"数据校验失败", "en:Data validation failed"};

    AppStubLocalImpl(AppContextImpl appContext, AppMeta appMeta) {
        this.appContext = appContext;
        this.appMeta = appMeta;
        this.i18nService = ServiceContainerProvider.getService(I18nService.class);
        this.validation = new Validation(i18nService);
    }

    @Override
    public AppMeta getAppMeta() {
        return appMeta;
    }

    @Override
    public AppContext getAppContext() {
        return appContext;
    }

    @Override
    public void invokeApp(RequestImpl request) throws Throwable {
        for (ActionFilter actionFilter : appContext.actionFilters) {
            String error = actionFilter.doFilter(request);
            if (error != null && !error.trim().isEmpty()) {
                error(request, error.trim());
                return;
            }
        }

        for (Model m : appMeta.getApp().models) {
            if (m.code.equals(request.getModel())) {
                for (ModelAction a : m.actions) {
                    if (a.code.equals(request.getAction())) {
                        request.setCurrentModel(m);

                        Map<String, List<String>> errors = validation.validate(a, request);
                        if (errors != null && !errors.isEmpty()) {
                            String langStr = request.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);
                            error(request, i18nService.getI18n(MSG_DATA_VALIDATION_FAILED, langStr));
                            request.getResponse().data(errors);
                            return;
                        }

                        invokeAction(m, a, request);
                        break;
                    }
                }
                break;
            }
        }
    }

    private void invokeAction(Model model, ModelAction action, RequestImpl request) throws Throwable {
        ModelBase modelBase = appContext.modelInstances.get(model);
        if (action.isDefaultAction) {
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
        } else {
            Method method = appContext.actionMethods.get(AppLoader.resolveActionKey(model, action));
            if (method != null) {
                invokeMethod(request, method, modelBase, request);
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

    private void error(RequestImpl request, String error) {
        ResponseImpl response = request.getResponse();
        response.status(400);
        response.success(false);
        response.msg(error);
        response.msgLevel(Response.MsgLevel.error);
    }
}