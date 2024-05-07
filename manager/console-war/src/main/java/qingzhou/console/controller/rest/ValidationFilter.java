package qingzhou.console.controller.rest;

import qingzhou.api.type.Createable;
import qingzhou.api.type.Editable;
import qingzhou.console.RequestImpl;
import qingzhou.console.ResponseImpl;
import qingzhou.console.controller.SystemController;
import qingzhou.console.i18n.ConsoleI18n;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.registry.AppInfo;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ValidationFilter implements Filter<RestContext> {
    public static String validation_required = "validation_required";
    public static String validation_min = "validation_min";
    public static String validation_max = "validation_max";

    static {
        ConsoleI18n.addI18n(validation_required, new String[]{"不支持为空", "en:Cannot be empty"});
        ConsoleI18n.addI18n(validation_min, new String[]{"不能小于%s", "en:It cannot be less than %s"});
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        Map<String, String> errorMsg = new HashMap<>();
        RequestImpl request = context.request;
        if (Createable.ACTION_NAME_ADD.equals(request.getAction())
                || Editable.ACTION_NAME_UPDATE.equals(request.getAction())) {
            AppInfo appInfo = SystemController.getAppInfo(request.getApp());
            ModelInfo modelInfo = appInfo.getModelInfo(request.getModel());
            clipParameter(request, modelInfo);
            for (String field : modelInfo.getFormFieldNames()) {
                ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
                String parameterVal = request.getParameter(field);
                String error = validate(fieldInfo, parameterVal);
                if (error != null) {
                    errorMsg.put(field, error);
                }
            }
        }

        ResponseImpl response = context.response;
        response.setSuccess(errorMsg.isEmpty());

        return response.isSuccess();
    }

    private void clipParameter(RequestImpl request, ModelInfo modelInfo) {
        List<String> toRemove = request.getParameters().keySet().stream().filter(param -> Arrays.stream(modelInfo.getFormFieldNames()).noneMatch(s -> s.equals(param))).collect(Collectors.toList());
        toRemove.forEach(request::removeParameter);
    }

    Validator[] validators = {
            new min(), new max() // todo: ModelField 的 lengthMin 及以后的校验需要加上
    };

    private String validate(ModelFieldInfo fieldInfo, String parameterVal) {
        if (parameterVal == null || parameterVal.trim().isEmpty()) {
            if (fieldInfo.isRequired()) {
                return validation_required;
            }
        }

        for (Validator validator : validators) {
            String error = validator.validate(fieldInfo, parameterVal);
            if (error != null) return error;
        }

        return null;
    }

    interface Validator {
        String validate(ModelFieldInfo fieldInfo, String parameterVal);
    }

    static class min implements Validator {

        @Override
        public String validate(ModelFieldInfo fieldInfo, String parameterVal) {
            if (fieldInfo.getMin() == Long.MIN_VALUE) return null;
            if (Long.parseLong(parameterVal) >= fieldInfo.getMin()) return null;
            return validation_min;
        }
    }

    static class max implements Validator {

        @Override
        public String validate(ModelFieldInfo fieldInfo, String parameterVal) {
            if (fieldInfo.getMax() == Long.MAX_VALUE) return null;
            if (Long.parseLong(parameterVal) <= fieldInfo.getMax()) return null;
            return validation_max;
        }
    }
}
