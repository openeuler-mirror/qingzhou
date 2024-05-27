package qingzhou.console.controller.rest;

import qingzhou.api.type.Createable;
import qingzhou.api.type.Editable;
import qingzhou.console.RequestImpl;
import qingzhou.console.ResponseImpl;
import qingzhou.console.i18n.ConsoleI18n;
import qingzhou.console.page.PageBackendService;
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
    public static String validation_lengthMin = "validation_lengthMin";
    public static String validation_lengthMax = "validation_lengthMax";
    public static String validation_port = "validation_port";
    public static String unsupportedCharacters = "unsupportedCharacters";
    public static String unsupportedStrings = "unsupportedStrings";
    public static String createable = "createable";
    public static String editable = "editable";

    static {
        ConsoleI18n.addI18n(validation_required, new String[]{"内容不能为空白", "en:The content cannot be blank"});
        ConsoleI18n.addI18n(validation_min, new String[]{"数字不能小于%s", "en:The number cannot be less than %s"});
        ConsoleI18n.addI18n(validation_max, new String[]{"数字不能大于%s", "en:The number cannot be greater than %s"});
        ConsoleI18n.addI18n(validation_lengthMin, new String[]{"字符串长度不能小于%s", "en:The length of the string cannot be less than %s"});
        ConsoleI18n.addI18n(validation_lengthMax, new String[]{"字符串长度不能大于%s", "en:The length of the string cannot be greater than %s"});
        ConsoleI18n.addI18n(validation_port, new String[]{"须是一个合法的端口", "en:Must be a legitimate port"});
        ConsoleI18n.addI18n(unsupportedStrings, new String[]{"不能包含字串%s", "en:Cannot contain the string %s"});
        ConsoleI18n.addI18n(createable, new String[]{"创建时不支持写入该属性", "en:Writing this property is not supported during creation"});
        ConsoleI18n.addI18n(editable, new String[]{"编辑时不支持写入该属性", "en:Writing this property is not supported during editing"});
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        Map<String, String> errorMsg = new HashMap<>();
        RequestImpl request = context.request;
        boolean isAddAction = Createable.ACTION_NAME_ADD.equals(request.getAction());
        boolean isUpdateAction = Editable.ACTION_NAME_UPDATE.equals(request.getAction());
        if (isAddAction || isUpdateAction) {
            AppInfo appInfo = PageBackendService.getAppInfo(PageBackendService.getAppName(request));
            ModelInfo modelInfo = appInfo.getModelInfo(request.getModel());
            clipParameter(request, modelInfo);
            for (String field : modelInfo.getFormFieldNames()) {
                ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
                String parameterVal = request.getParameter(field);
                ValidationContext vc = new ValidationContext(fieldInfo, parameterVal, isAddAction, isUpdateAction);
                String[] error = validate(vc);
                if (error != null) {
                    String[] params = Arrays.copyOfRange(error, 1, error.length);
                    String i18n = ConsoleI18n.getI18n(request.getLang(), error[0], (Object) params);
                    errorMsg.put(field, i18n);
                }
            }
        }

        ResponseImpl response = context.response;
        response.setSuccess(errorMsg.isEmpty());
        response.addData(errorMsg);

        return response.isSuccess();
    }

    private void clipParameter(RequestImpl request, ModelInfo modelInfo) {
        List<String> toRemove = request.getParameters().keySet().stream().filter(param -> Arrays.stream(modelInfo.getFormFieldNames()).noneMatch(s -> s.equals(param))).collect(Collectors.toList());
        toRemove.forEach(request::removeParameter);
    }

    Validator[] validators = {
            new min(), new max(),
            new lengthMin(), new lengthMax(),
            new port(),
            new unsupportedCharacters(), new unsupportedStrings(),
            new createable(), new editable()
    };

    private String[] validate(ValidationContext context) throws Exception {
        ModelFieldInfo fieldInfo = context.fieldInfo;
        String parameterVal = context.parameterVal;
        if (parameterVal == null || parameterVal.isEmpty()) {
            if (context.isAddAction && !fieldInfo.isCreateable()) {
                return null;
            }
            if (context.isUpdateAction && !fieldInfo.isEditable()) {
                return null;
            }
            if (fieldInfo.isRequired() && PageBackendService.isShow(o -> parameterVal, fieldInfo.getShow())) {
                return new String[]{validation_required};
            }
        }

        for (Validator validator : validators) {
            String[] error = validator.validate(context);
            if (error != null) return error;
        }

        return null;
    }

    interface Validator {
        String[] validate(ValidationContext context);
    }

    static class ValidationContext {
        final ModelFieldInfo fieldInfo;
        final String parameterVal;
        final boolean isAddAction;
        final boolean isUpdateAction;

        ValidationContext(ModelFieldInfo fieldInfo, String parameterVal, boolean isAddAction, boolean isUpdateAction) {
            this.fieldInfo = fieldInfo;
            this.parameterVal = parameterVal;
            this.isAddAction = isAddAction;
            this.isUpdateAction = isUpdateAction;
        }
    }

    static class min implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            String parameterVal = context.parameterVal;
            if (fieldInfo.getMin() == Long.MIN_VALUE) return null;
            if (Long.parseLong(parameterVal) >= fieldInfo.getMin()) return null;
            return new String[]{validation_min, String.valueOf(fieldInfo.getMin())};
        }
    }

    static class max implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            String parameterVal = context.parameterVal;
            if (fieldInfo.getMax() == Long.MAX_VALUE) return null;
            if (Long.parseLong(parameterVal) <= fieldInfo.getMax()) return null;
            return new String[]{validation_max, String.valueOf(fieldInfo.getMax())};
        }
    }

    static class lengthMin implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            String parameterVal = context.parameterVal;
            if (fieldInfo.getLengthMin() == -1) return null;
            if (parameterVal.length() >= fieldInfo.getLengthMin()) return null;
            return new String[]{validation_lengthMin, String.valueOf(fieldInfo.getLengthMin())};
        }
    }

    static class lengthMax implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            String parameterVal = context.parameterVal;
            if (fieldInfo.getLengthMax() == Integer.MAX_VALUE) return null;
            if (parameterVal.length() <= fieldInfo.getLengthMax()) return null;
            return new String[]{validation_lengthMax, String.valueOf(fieldInfo.getLengthMax())};
        }
    }

    static class port implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            String parameterVal = context.parameterVal;
            if (!fieldInfo.isPort()) return null;
            return new String[]{validation_port};
        }
    }

    static class unsupportedCharacters implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            String parameterVal = context.parameterVal;
            if (fieldInfo.getUnsupportedCharacters().isEmpty()) return null;
            for (char c : fieldInfo.getUnsupportedCharacters().toCharArray()) {
                String s = String.valueOf(c);
                if (parameterVal.contains(s)) {
                    return new String[]{unsupportedCharacters, s};
                }
            }
            return null;
        }
    }

    static class unsupportedStrings implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            String parameterVal = context.parameterVal;
            for (String unsupportedString : fieldInfo.getUnsupportedStrings()) {
                if (parameterVal.contains(unsupportedString)) {
                    return new String[]{unsupportedStrings, unsupportedString};
                }
            }
            return null;
        }
    }

    static class createable implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (fieldInfo.isCreateable()) return null;

            if (!context.isAddAction) return null;

            return new String[]{createable};
        }
    }

    static class editable implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (fieldInfo.isEditable()) return null;

            if (!context.isUpdateAction) return null;

            return new String[]{editable};
        }
    }

}
