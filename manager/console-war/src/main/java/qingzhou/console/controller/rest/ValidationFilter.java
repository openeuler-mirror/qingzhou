package qingzhou.console.controller.rest;

import qingzhou.api.FieldType;
import qingzhou.api.Response;
import qingzhou.api.type.Add;
import qingzhou.api.type.List;
import qingzhou.api.type.Update;
import qingzhou.api.type.Validate;
import qingzhou.console.SecurityController;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidationFilter implements Filter<RestContext> {
    static {
        I18n.addKeyI18n("validation_id", new String[]{"已存在", "en:Already exists"});
        I18n.addKeyI18n("validation_required", new String[]{"内容不能为空白", "en:The content cannot be blank"});
        I18n.addKeyI18n("validation_number", new String[]{"须是数字类型", "en:Must be a numeric type"});
        I18n.addKeyI18n("validation_min", new String[]{"数字不能小于%s", "en:The number cannot be less than %s"});
        I18n.addKeyI18n("validation_max", new String[]{"数字不能大于%s", "en:The number cannot be greater than %s"});
        I18n.addKeyI18n("validation_lengthMin", new String[]{"字符串长度不能小于%s", "en:The length of the string cannot be less than %s"});
        I18n.addKeyI18n("validation_lengthMax", new String[]{"字符串长度不能大于%s", "en:The length of the string cannot be greater than %s"});
        I18n.addKeyI18n("validation_host", new String[]{"非法的IP地址或域名", "en:Illegal IP address or host name"});
        I18n.addKeyI18n("validation_port", new String[]{"须是一个合法的端口", "en:Must be a legitimate port"});
        I18n.addKeyI18n("validation_port_valueBetween", new String[]{"取值必须介于%s - %s之间", "en:Value must be between %s and %s"});
        I18n.addKeyI18n("validation_unsupportedCharacters", new String[]{"不能包含字符：%s", "en:Cannot contain the char: %s"});
        I18n.addKeyI18n("validation_unsupportedStrings", new String[]{"该值已被禁用", "en:This value is disabled"});
        I18n.addKeyI18n("validation_createable", new String[]{"创建时不支持写入该属性", "en:Writing this property is not supported during creation"});
        I18n.addKeyI18n("validation_xss", new String[]{"可能存在XSS风险或隐患", "en:There may be XSS risks or hidden dangers"});
        I18n.addKeyI18n("validation_pattern", new String[]{"内容不满足规则", "en:The content does not meet the rules"});
        I18n.addKeyI18n("validation_email", new String[]{"须是一个合法的电子邮件地址", "en:Must be a valid email address"});
        I18n.addKeyI18n("validation_filePath", new String[]{"文件路径不支持以\\或者/结尾，不支持包含特殊字符和空格", "en:The file path cannot end with \\ or /, and cannot contain special characters or Spaces"});
        I18n.addKeyI18n("validation_options", new String[]{"取值不在范围：%s", "en:The value is not in the range: %s"});
        I18n.addKeyI18n("validation_datetime", new String[]{"须符合时间格式：%s", "en:Must conform to the time format: %s"});
        I18n.addKeyI18n("validation_readonly", new String[]{"当前状态下不支持写入", "en:Writes are not supported in the current state"});
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        Map<String, String> errorMsg = new HashMap<>();
        RequestImpl request = context.request;

        ModelInfo modelInfo = request.getCachedModelInfo();

        boolean isAddAction = Add.ACTION_ADD.equals(request.getAction());
        boolean isUpdateAction = Update.ACTION_UPDATE.equals(request.getAction());
        if (isAddAction || isUpdateAction) {
            for (String field : modelInfo.getFormFieldNames()) {
                ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
                String parameterVal = request.getParameter(field);
                ValidationContext vc = new ValidationContext(request, modelInfo, fieldInfo, parameterVal, isAddAction, isUpdateAction);
                String[] error = validate(vc);
                if (error != null) {
                    Object[] params = null;
                    if (error.length > 1) {
                        params = Arrays.copyOfRange(error, 1, error.length);
                    }
                    String i18n = I18n.getKeyI18n(error[0], params);
                    errorMsg.put(field, i18n);
                }
            }

            // 是否需要自定义校验
            if (modelInfo.isValidate()) {
                RequestImpl tmp = new RequestImpl(context.request);
                tmp.setActionName(Validate.ACTION_VALIDATE);
                context.request.getParameters().forEach(tmp::setParameter);
                tmp.setNonModelParameter(Validate.IS_ADD_OR_UPDATE_NON_MODEL_PARAMETER, String.valueOf(isAddAction));
                Response tmpResp = SystemController.getService(ActionInvoker.class).invokeSingle(tmp);
                if (!tmpResp.isSuccess()) {
                    java.util.List<Map<String, String>> dataList = tmpResp.getDataList();
                    if (!dataList.isEmpty()) {
                        errorMsg.putAll(dataList.get(0));
                    }
                }
            }
        }

        Response response = request.getResponse();
        if (!errorMsg.isEmpty()) {
            response.setSuccess(false);
            response.addData(errorMsg);
        }

        return response.isSuccess();
    }

    Validator[] validators = {
            new id(),
            new min(), new max(),
            new lengthMin(), new lengthMax(),
            new host(),
            new port(),
            new unsupportedCharacters(), new unsupportedStrings(),
            new createable(), new editable(),
            new checkXSS(),
            new regularExpression(),
            new checkEmail(),
            new checkFilePath(),
            new options(),
            new datetime(),
            new readonly()
    };

    private String[] validate(ValidationContext context) {
        ModelFieldInfo fieldInfo = context.fieldInfo;

        boolean show = SecurityController.checkRule(fieldInfo.getShow(), context.request::getParameter, true);
        if (!show) { // 不再页面显示的属性，不需要校验，并删除之以免后续持久化了错误数据
            context.request.removeParameter(fieldInfo.getCode());
            return null;
        }

        // 处理空值情况
        if (context.parameterVal == null || context.parameterVal.isEmpty()) {
            if (context.isAddAction && !fieldInfo.isCreate()) {
                return null;
            }
            if (context.isUpdateAction && !fieldInfo.isEdit()) {
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
        final RequestImpl request;
        final ModelFieldInfo fieldInfo;
        final ModelInfo modelInfo;
        final String parameterVal;
        final boolean isAddAction;
        final boolean isUpdateAction;

        ValidationContext(RequestImpl request, ModelInfo modelInfo, ModelFieldInfo fieldInfo, String parameterVal, boolean isAddAction, boolean isUpdateAction) {
            this.request = request;
            this.modelInfo = modelInfo;
            this.fieldInfo = fieldInfo;
            this.parameterVal = parameterVal;
            this.isAddAction = isAddAction;
            this.isUpdateAction = isUpdateAction;
        }
    }

    static class id implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            if (!context.fieldInfo.getCode().equals(context.modelInfo.getIdField())) return null;

            if (context.parameterVal.contains(DeployerConstants.BATCH_ID_SEPARATOR)) {
                return new String[]{"validation_unsupportedCharacters", DeployerConstants.BATCH_ID_SEPARATOR};
            }

            if (context.isAddAction) {
                RequestImpl tmp = new RequestImpl(context.request);
                tmp.setActionName(List.ACTION_CONTAINS);
                tmp.setId(context.parameterVal);
                Response tmpResp = SystemController.getService(ActionInvoker.class).invokeSingle(tmp);
                boolean success = tmpResp.isSuccess();
                if (success) {
                    return new String[]{"validation_id"};
                }
            }

            return null;
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

    static class host implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (!fieldInfo.isHost()) return null;
            // 兼容这个情况： newValue == 0.0.0.0aa
            try {
                InetAddress.getByName(context.parameterVal).getHostAddress(); // ::1简写变成完整名称
            } catch (UnknownHostException ignored) {
                return new String[]{"validation_host"};
            }

            return null;
        }
    }

    static class unsupportedCharacters implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (fieldInfo.getNoChar().isEmpty()) return null;
            for (char c : fieldInfo.getNoChar().toCharArray()) {
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
            for (String unsupportedString : context.fieldInfo.getNoString()) {
                for (String param : context.parameterVal.split(DeployerConstants.DEFAULT_DATA_SEPARATOR)) {
                    if (param.equals(unsupportedString)) {
                        return new String[]{"validation_unsupportedStrings"};
                    }
                }
            }
            return null;
        }
    }

    static class createable implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (fieldInfo.isCreate()) return null;

            if (!context.isAddAction) return null;

            return new String[]{"validation_createable"};
        }
    }

    static class editable implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            if (fieldInfo.isEdit()) return null;

            if (!context.isUpdateAction) return null;

            // context.request.removeParameter(fieldInfo.getCode()); 有应用里需要这些参数，不能移除
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
                    || fieldInfo.isFile()) {
                String[] illegalCollections = {"|", "&", "~", "../", "./", "*", "?", "\"", "'", "<", ">", "(", ")", "[", "]", "{", "}", "^", " "};
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

    static class options implements Validator {
        @Override
        public String[] validate(ValidationContext context) {
            String[] options = SystemController.getOptions(context.request.getApp(), context.fieldInfo);
            if (options == null || options.length == 0) return null;
            String[] vals = context.parameterVal.split(context.fieldInfo.getSeparator());
            for (String v : vals) {
                boolean match = Arrays.asList(options).contains(v);
                if (!match) {
                    return new String[]{"validation_options", Arrays.toString(options)};
                }
            }
            return null;
        }
    }

    static class datetime implements Validator {
        @Override
        public String[] validate(ValidationContext context) {
            if (!FieldType.datetime.name().equals(context.fieldInfo.getType())) return null;

            try {
                new SimpleDateFormat(DeployerConstants.FIELD_DATETIME_FORMAT).parse(context.parameterVal);
                return null;
            } catch (ParseException e) {
                return new String[]{"validation_datetime", DeployerConstants.FIELD_DATETIME_FORMAT};
            }
        }
    }

    static class readonly implements Validator {
        @Override
        public String[] validate(ValidationContext context) {
            if (context.isAddAction) return null;

            String readOnly = context.fieldInfo.getReadOnly();
            if (readOnly == null || readOnly.trim().isEmpty()) return null;

            boolean isReadOnly = SecurityController.checkRule(readOnly, context.request::getParameter, false);
            if (isReadOnly) {
                RequestImpl tmp = new RequestImpl(context.request);
                tmp.setActionName(Update.ACTION_CHANGED);
                tmp.setNonModelParameter(DeployerConstants.CHANGED_KEY, context.fieldInfo.getCode());
                tmp.setNonModelParameter(DeployerConstants.CHANGED_VAL, context.parameterVal);
                Response tmpResp = SystemController.getService(ActionInvoker.class).invokeSingle(tmp);
                boolean changed = tmpResp.isSuccess();
                if (!changed) {
                    return new String[]{"validation_readonly"};
                }
            }

            return null;
        }
    }
}
