package qingzhou.console.controller.rest;

import qingzhou.api.FieldType;
import qingzhou.api.Response;
import qingzhou.console.controller.SystemController;
import qingzhou.console.i18n.ConsoleI18n;
import qingzhou.console.page.PageBackendService;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.registry.AppInfo;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ValidationFilter implements Filter<RestContext> {
    static {
        ConsoleI18n.addI18n("validation_required", new String[]{"内容不能为空白", "en:The content cannot be blank"});
        ConsoleI18n.addI18n("validation_number", new String[]{"须是数字类型", "en:Must be a numeric type"});
        ConsoleI18n.addI18n("validation_min", new String[]{"数字不能小于%s", "en:The number cannot be less than %s"});
        ConsoleI18n.addI18n("validation_max", new String[]{"数字不能大于%s", "en:The number cannot be greater than %s"});
        ConsoleI18n.addI18n("validation_lengthMin", new String[]{"字符串长度不能小于%s", "en:The length of the string cannot be less than %s"});
        ConsoleI18n.addI18n("validation_lengthMax", new String[]{"字符串长度不能大于%s", "en:The length of the string cannot be greater than %s"});
        ConsoleI18n.addI18n("validation_port", new String[]{"须是一个合法的端口", "en:Must be a legitimate port"});
        ConsoleI18n.addI18n("validation_port_valueBetween", new String[]{"取值必须介于%s - %s之间", "en:Value must be between %s and %s"});
        ConsoleI18n.addI18n("validation_unsupportedCharacters", new String[]{"不能包含字符：%s", "en:Cannot contain the char: %s"});
        ConsoleI18n.addI18n("validation_unsupportedStrings", new String[]{"不能包含字符串：%s", "en:Cannot contain the string: %s"});
        ConsoleI18n.addI18n("validation_createable", new String[]{"创建时不支持写入该属性", "en:Writing this property is not supported during creation"});
        ConsoleI18n.addI18n("validation_xss", new String[]{"可能存在XSS风险或隐患", "en:There may be XSS risks or hidden dangers"});
        ConsoleI18n.addI18n("validation_pattern", new String[]{"内容不满足规则", "en:The content does not meet the rules"});
        ConsoleI18n.addI18n("validation_email", new String[]{"须是一个合法的电子邮件地址", "en:Must be a valid email address"});
        ConsoleI18n.addI18n("validation_filePath", new String[]{"文件路径不支持以\\或者/结尾，不支持包含特殊字符和空格", "en:The file path cannot end with \\ or /, and cannot contain special characters or Spaces"});
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        Map<String, String> errorMsg = new HashMap<>();
        RequestImpl request = context.request;
        boolean isAddAction = "add".equals(request.getAction());
        boolean isUpdateAction = "update".equals(request.getAction());
        if (isAddAction || isUpdateAction) {
            AppInfo appInfo = SystemController.getAppInfo(PageBackendService.getAppName(request));
            ModelInfo modelInfo = appInfo.getModelInfo(request.getModel());
            clipParameter(request, modelInfo);
            Map<String, String> paramMap = request.getParameters();
            for (String field : modelInfo.getFormFieldNames()) {
                ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
                String parameterVal = request.getParameter(field);
                ValidationContext vc = new ValidationContext(paramMap, modelInfo, fieldInfo, parameterVal, isAddAction, isUpdateAction);
                String[] error = validate(vc);
                if (error != null) {
                    Object[] params = null;
                    if (error.length > 1) {
                        params = Arrays.copyOfRange(error, 1, error.length);
                    }
                    String i18n = ConsoleI18n.getI18n(request.getLang(), error[0], params);
                    errorMsg.put(field, i18n);
                }
            }
        }

        Response response = context.request.getResponse();
        if (!errorMsg.isEmpty()) {
            response.setSuccess(false);
            response.addData(errorMsg);
        }

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
            new createable(), new editable(),
            new checkXSS(),
            new regularExpression(),
            new checkEmail(),
            new checkFilePath()
    };

    private String[] validate(ValidationContext context) throws Exception {
        Map<String, String> paramMap = context.params;
        ModelFieldInfo fieldInfo = context.fieldInfo;

        boolean show = PageBackendService.isShow(paramMap::get, fieldInfo.getShow());
        if (!show) { // 不再页面显示的属性，不需要校验
            return null;
        }

        // 处理空值情况
        if (context.parameterVal == null || context.parameterVal.isEmpty()) {
            if (context.isAddAction && !fieldInfo.isCreateable()) {
                return null;
            }
            if (context.isUpdateAction && !fieldInfo.isEditable()) {
                return null;
            }
            if (fieldInfo.isRequired()) {
                return new String[]{"validation_required"};
            } else {
                return null;
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
        final ModelInfo modelInfo;
        final ModelFieldInfo fieldInfo;
        final String parameterVal;
        final boolean isAddAction;
        final boolean isUpdateAction;
        final Map<String, String> params;

        ValidationContext(Map<String, String> params, ModelInfo modelInfo, ModelFieldInfo fieldInfo, String parameterVal, boolean isAddAction, boolean isUpdateAction) {
            this.modelInfo = modelInfo;
            this.params = params;
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
            try {
                if (!FieldType.number.name().equals(fieldInfo.getType())) return null;
                if (Long.parseLong(context.parameterVal) >= fieldInfo.getMin()) return null;
                return new String[]{"validation_min", String.valueOf(fieldInfo.getMin())};
            } catch (Exception e) {
                return new String[]{"validation_number"};
            }
        }
    }

    static class max implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            try {
                if (!FieldType.number.name().equals(fieldInfo.getType())) return null;
                if (Long.parseLong(context.parameterVal) <= fieldInfo.getMax()) return null;
                return new String[]{"validation_max", String.valueOf(fieldInfo.getMax())};
            } catch (Exception e) {
                return new String[]{"validation_number"};
            }
        }
    }

    static class lengthMin implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (context.parameterVal.length() >= fieldInfo.getLengthMin()) return null;
            return new String[]{"validation_lengthMin", String.valueOf(fieldInfo.getLengthMin())};
        }
    }

    static class lengthMax implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (context.parameterVal.length() <= fieldInfo.getLengthMax()) return null;
            return new String[]{"validation_lengthMax", String.valueOf(fieldInfo.getLengthMax())};
        }
    }

    static class regularExpression implements Validator {
        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (Utils.isBlank(fieldInfo.getPattern())) return null;
            Pattern pattern = Pattern.compile(fieldInfo.getPattern());
            Matcher matcher = pattern.matcher(context.parameterVal);
            if (matcher.matches()) return null;
            return new String[]{"validation_pattern"};
        }
    }

    static class port implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (!fieldInfo.isPort()) return null;
            try {
                long guessNumber = Long.parseLong(context.parameterVal);
                int min = 1;
                int max = 65535;
                if (guessNumber < min || guessNumber > max) {
                    return new String[]{"validation_port_valueBetween", String.valueOf(min), String.valueOf(max)};
                }
            } catch (Exception e) {
                return new String[]{"validation_port"};
            }

            return null;
        }
    }

    static class unsupportedCharacters implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (fieldInfo.getUnsupportedCharacters().isEmpty()) return null;
            for (char c : fieldInfo.getUnsupportedCharacters().toCharArray()) {
                String s = String.valueOf(c);
                if (context.parameterVal.contains(s)) {
                    return new String[]{"validation_unsupportedCharacters", s};
                }
            }
            return null;
        }
    }

    static class unsupportedStrings implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            for (String unsupportedString : context.fieldInfo.getUnsupportedStrings()) {
                if (context.parameterVal.contains(unsupportedString)) {
                    return new String[]{"validation_unsupportedStrings", unsupportedString};
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

            return new String[]{"validation_createable"};
        }
    }

    static class editable implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (fieldInfo.isEditable()) return null;

            if (!context.isUpdateAction) return null;

            context.params.remove(fieldInfo.getCode());
            return null;
        }
    }

    static class checkXSS implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            if (!checkIsXSS(context.parameterVal)) return null;
            return new String[]{"validation_xss"};
        }

        final Pattern scriptPattern1 = Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE);

        boolean checkIsXSS(String check) {
            return !checkXssOk(check);
        }

        boolean checkXssLevel1(String check) {
            if (check == null || check.trim().isEmpty()) {
                return true;
            } else {
                String resultUrl = check.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
                if (!resultUrl.equals(check)) {
                    return false;
                } else {
                    resultUrl = resultUrl.replaceAll("eval\\((.*)\\)", "");
                    if (!resultUrl.equals(check)) {
                        return false;
                    } else {
                        resultUrl = scriptPattern1.matcher(resultUrl).replaceAll("");
                        if (!resultUrl.equals(check)) {
                            return false;
                        } else {
                            return !resultUrl.contains("'") && !resultUrl.contains("\"") || resultUrl.indexOf(")") <= resultUrl.indexOf("(");
                        }
                    }
                }
            }
        }

        boolean checkXssOk(String check) {
            if (check == null || check.trim().isEmpty()) {
                return true;
            } else if (!checkXssLevel1(check)) {
                return false;
            } else {
                String resultUrl = check.replaceAll("\\(", "&#40").replaceAll("\\)", "&#41");
                if (!resultUrl.equals(check)) {
                    return false;
                } else {
                    resultUrl = resultUrl.replaceAll("\\[", "&#91").replaceAll("]", "&#93");
                    return resultUrl.equals(check);
                }
            }
        }
    }

    static class checkEmail implements Validator {
        // 邮箱正则表达式
        private static final String EMAIL_PATTERN =
                "^[A-Za-z0-9]+([-._][A-Za-z0-9]+)*@[A-Za-z0-9]+(-[A-Za-z0-9]+)*(\\.[A-Za-z]{2,6}|[A-Za-z]{2,4}\\.[A-Za-z]{2,3})$";
        private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (!fieldInfo.isEmail()) return null;
            Matcher matcher = pattern.matcher(context.parameterVal);
            if (matcher.matches()) return null;
            return new String[]{"validation_email"};
        }
    }

    static class checkFilePath implements Validator {
        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (fieldInfo.getType().equals(FieldType.file.name())
                    || fieldInfo.isFilePath()) {
                String[] illegalCollections = {"|", "&", "~", "../", "./", ":", "*", "?", "\"", "'", "<", ">", "(", ")", "[", "]", "{", "}", "^", " "};
                for (String illegalCollection : illegalCollections) {
                    if (context.parameterVal.contains(illegalCollection)) {
                        return new String[]{"validation_filePath"};
                    }
                }
                try {
                    new File(context.parameterVal);
                } catch (Exception e) {
                    return new String[]{"validation_filePath"};
                }
            }
            return null;
        }
    }
}
