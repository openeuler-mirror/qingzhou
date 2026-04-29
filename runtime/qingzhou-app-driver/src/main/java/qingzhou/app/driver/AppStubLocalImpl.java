package qingzhou.app.driver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Reference;
import qingzhou.api.*;
import qingzhou.app.driver.validation.Validation;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.I18nService;

class AppStubLocalImpl implements AppStubLocal {
    private final Validation validation = new Validation();

    private final AppContextImpl appContext;
    private final AppMeta appMeta;
    private I18nService i18nService;

    // Error messages
    private static final String[] MSG_DATA_VALIDATION_FAILED = {"数据校验失败", "en:Data validation failed"};

    AppStubLocalImpl(AppContextImpl appContext, AppMeta appMeta) {
        this.appContext = appContext;
        this.appMeta = appMeta;
    }

    @Reference
    public void setI18nService(I18nService i18nService) {
        this.i18nService = i18nService;
        validation.setI18nService(i18nService);
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
        // 确保应用的拦截器总是可以被执行
        for (ActionFilter actionFilter : appContext.getActionFilters()) {
            String error = actionFilter.doFilter(request);
            if (error != null) {
                error(request, error);
                return;
            }
        }

        // 查找并执行 Action
        for (Model m : appMeta.getApp().models) {
            if (m.code.equals(request.getModel())) {
                for (ModelAction a : m.actions) {
                    if (a.code.equals(request.getAction())) {
                        request.setCurrentModel(m);

                        // 数据校验
                        Map<String, List<String>> errors = validation.validate(a, request);
                        if (errors != null && !errors.isEmpty()) {
                            String langStr = request.getParameter("lang");
                            Lang lang;
                            try {
                                lang = langStr != null ? Lang.valueOf(langStr) : Lang.zh;
                            } catch (IllegalArgumentException e) {
                                lang = Lang.zh;
                            }
                            error(request, i18nService.getI18n(MSG_DATA_VALIDATION_FAILED, lang));
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
        ModelBase modelBase = appContext.getModelInstances().get(model);
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
            Method method = appContext.getActionMethods().get(AppContextImpl.resolveActionKey(model, action));
            if (method != null) {
                invokeMethod(request, method, modelBase, request);
            }
        }
    }

    private void invokeMethod(RequestImpl request, Method method, Object obj, Object... args) throws Throwable {
        ResponseImpl response = request.getResponse();
        response.setActionFound(true);
        try {
            method.invoke(obj, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private void error(RequestImpl request, String error) {
        ResponseImpl response = request.getResponse();
        response.status(400); // 请求参数、格式等不合法
        response.success(false);
        response.msg(error);
        response.msgLevel(Response.MsgLevel.error);
    }
}