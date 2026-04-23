package qingzhou.app.driver;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import qingzhou.api.*;
import qingzhou.api.type.Add;
import qingzhou.api.type.Update;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.registry.AppStubLocal;

class AppStubLocalImpl implements AppStubLocal {
    private final AppContextImpl appContext;
    private final AppMeta appMeta;

    AppStubLocalImpl(AppContextImpl appContext, AppMeta appMeta) {
        this.appContext = appContext;
        this.appMeta = appMeta;
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
                        // 确认已找到目标 Action
                        request.getResponse().setFound(true);
                        request.setCurrentModel(m);

                        // 数据校验
                        Map<String, String> errors = validate(a, request);
                        if (errors != null && !errors.isEmpty()) {
//                            error(request, error);  todo 转json
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
        if (action.isDefaultAction) {
            for (Method method : DefaultAction.class.getMethods()) {
                if (method.getName().equals(action.code)) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == 2) {
                        if (QingzhouModel.class.isAssignableFrom(parameterTypes[0])
                                && parameterTypes[1] == Request.class) {
                            method.invoke(null, modelBase, request);
                        }
                    }
                    break;
                }
            }
        } else { // 执行自定义的 action
            Method method = appContext.getActionMethods().get(AppContextImpl.resolveActionKey(model, action));
            if (method != null) {
                method.invoke(modelBase, request);
            }
        }
    }

    private void error(RequestImpl request, String error) {
        ResponseImpl response = request.getResponse();
        response.success(false);
        response.msg(error);
        response.msgLevel(Response.MsgLevel.error);
    }

    private Validator[] validators = {};

    private Map<String, String> validate(ModelAction action, RequestImpl request) {
        // 是否开启了校验
        if (action.skip_validation) return null;

        // 确定要检验哪些字段
        Set<ModelField> validateFields = null;
        if (action.code.equals(Add.ACTION_CODE_ADD)) {
            validateFields = request.getCurrentModel().fields.stream().filter(field -> field.field_type == FieldType.FORM && field.add).collect(Collectors.toSet());
        } else if (action.code.equals(Update.ACTION_CODE_UPDATE)) {
            validateFields = request.getCurrentModel().fields.stream().filter(field -> field.field_type == FieldType.FORM && field.update).collect(Collectors.toSet());
            validateFields.removeIf(field -> request.getParameter(field.code) == null); // rest 更新，可以指定要更新的字段，而无需全量字段
        }
        if (validateFields == null || validateFields.isEmpty()) return null;

        // 开始校验，一次性校验所有字段
        for (ModelField field : validateFields) {
            Map<String, String> result = new HashMap<>();
            ValidationContext context = new ValidationContext(field, request.getParameter(field.code), request);
            for (Validator validator : validators) {
                String error = validator.validate(context);
                if (error != null) {
                    result.put(field.code, error);
                }
            }
        }

        return null;
    }

    static class ValidationContext {
        final ModelField field;
        final String parameter;
        final RequestImpl request;

        ValidationContext(ModelField field, String parameter, RequestImpl request) {
            this.field = field;
            this.parameter = parameter;
            this.request = request;
        }
    }

    interface Validator {
        String validate(ValidationContext context);
    }
}