package qingzhou.console.controller.rest;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import qingzhou.api.InputType;
import qingzhou.api.type.Add;
import qingzhou.api.type.List;
import qingzhou.api.type.Update;
import qingzhou.api.type.Validate;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.core.DeployerConstants;
import qingzhou.core.ItemData;
import qingzhou.core.deployer.ActionInvoker;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
import qingzhou.core.registry.ModelActionInfo;
import qingzhou.core.registry.ModelFieldInfo;
import qingzhou.core.registry.ModelInfo;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;

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
        I18n.addKeyI18n("validation_forbid", new String[]{"该值已被禁用：%s", "en:This value is disabled: %s"});
        I18n.addKeyI18n("validation_xss", new String[]{"可能存在XSS风险或隐患", "en:There may be XSS risks or hidden dangers"});
        I18n.addKeyI18n("validation_pattern", new String[]{"内容不满足规则", "en:The content does not meet the rules"});
        I18n.addKeyI18n("validation_email", new String[]{"须是一个合法的电子邮件地址", "en:Must be a valid email address"});
        I18n.addKeyI18n("validation_filePath", new String[]{"文件路径不支持以\\或者/结尾，不支持包含特殊字符和空格", "en:The file path cannot end with \\ or /, and cannot contain special characters or Spaces"});
        I18n.addKeyI18n("validation_options", new String[]{"取值不在范围：%s", "en:The value is not in the range: %s"});
        I18n.addKeyI18n("validation_datetime", new String[]{"须符合时间格式：%s", "en:Must conform to the time format: %s"});
    }

    public static boolean isSingleSelect(ModelFieldInfo fieldInfo) {
        InputType type = fieldInfo.getInputType();
        return type == InputType.bool
                || type == InputType.radio
                || type == InputType.select;
    }

    public static boolean isMultipleSelect(ModelFieldInfo fieldInfo) {
        InputType type = fieldInfo.getInputType();
        return type == InputType.checkbox
                || type == InputType.sortable_checkbox
                || type == InputType.multiselect
                || type == InputType.sortable
                || type == InputType.kv;
    }

    public static boolean filterPageIsMultipleSelect(ModelFieldInfo fieldInfo) {
        return isSingleSelect(fieldInfo) || isMultipleSelect(fieldInfo);
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        Map<String, String> errorMsg = new HashMap<>();
        RequestImpl request = context.request;
        ResponseImpl response = (ResponseImpl) request.getResponse();

        ModelInfo modelInfo = request.getCachedModelInfo();

        boolean isAddAction = Add.ACTION_ADD.equals(request.getAction());
        boolean isUpdateAction = Update.ACTION_UPDATE.equals(request.getAction());
        if (isAddAction || isUpdateAction) {
            for (String field : modelInfo.getFormFieldNames()) {
                ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
                if (fieldInfo.isSkipValidate()) continue;
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
                context.request.getParameters().forEach((k, v) -> tmp.getParameters().put(k, v));
                tmp.getParameters().put(DeployerConstants.VALIDATION_ADD_FLAG, String.valueOf(isAddAction));
                ResponseImpl tmpResp = (ResponseImpl) SystemController.getService(ActionInvoker.class).invokeAny(tmp);
                Map<String, String> result = (Map<String, String>) tmpResp.getInternalData();
                errorMsg.putAll(result);
            }
        }

        if (!errorMsg.isEmpty()) {
            response.setSuccess(false);
            response.setInternalData(new HashMap<>(errorMsg));
        }

        return response.isSuccess();
    }

    Validator[] validators = {
            new id(),
            new min(), new max(),
            new lengthMin(), new lengthMax(),
            new host(),
            new port(),
            new forbid(),
            new checkXSS(),
            new regularExpression(),
            new checkEmail(),
            new checkFilePath(),
            new options(),
            new datetime(),
            new range_datetime()
    };

    private String[] validate(ValidationContext context) {
        ModelFieldInfo fieldInfo = context.fieldInfo;

        // 处理空值情况
        if (context.parameterVal == null || context.parameterVal.isEmpty()) {
            if (context.parameterVal == null
                    && context.isUpdateAction) { // rest 编辑，很多值都不传，只传递要修改的值
                return null;
            }

            if (context.isAddAction) {
                String editable = fieldInfo.getDisplay();
                if (Utils.notBlank(editable)) {
                    if (!SecurityController.checkRule(editable, context.request::getParameter)) {
                        return null;
                    }
                }
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

            if (context.isAddAction) {
                Boolean contains = contains(context.request, context.parameterVal);
                if (contains != null && contains) {
                    return new String[]{"validation_id"};
                }
            }

            return null;
        }
    }

    static Boolean contains(RequestImpl request, String id) {
        ModelInfo modelInfo = request.getCachedModelInfo();
        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(List.ACTION_CONTAINS);
        if (actionInfo == null) return null; // 没有提供实现
        RequestImpl tmp = new RequestImpl(request);
        tmp.setActionName(List.ACTION_CONTAINS);
        tmp.setId(id);
        ResponseImpl tmpResp = (ResponseImpl) SystemController.getService(ActionInvoker.class).invokeAny(tmp);
        return tmpResp.isSuccess();
    }

    static class min implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            ModelFieldInfo fieldInfo = context.fieldInfo;
            try {
                if (InputType.number != fieldInfo.getInputType()) return null;
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
                if (InputType.number != fieldInfo.getInputType()) return null;
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
                String hostAddress = InetAddress.getByName(context.parameterVal).getHostAddress();// ::1简写变成完整名称
                if (hostAddress == null) {
                    return new String[]{"validation_host"};
                }
            } catch (UnknownHostException ignored) {
                return new String[]{"validation_host"};
            }

            return null;
        }
    }

    static class forbid implements Validator {

        @Override
        public String[] validate(ValidationContext context) {
            for (String forbidString : context.fieldInfo.getForbid()) {
                for (String param : context.parameterVal.split(context.fieldInfo.getSeparator())) {
                    if (param.equals(forbidString)) {
                        return new String[]{"validation_forbid", forbidString};
                    }
                }
            }
            return null;
        }
    }

    static class checkXSS implements Validator {
        @Override
        public String[] validate(ValidationContext context) {
            String[] checks = {"vbscript:", "eval(", "(", ")", "<", ">", "[", "]", "\"", "'"};
            for (String check : checks) {
                if (context.parameterVal.contains(check)) {
                    if (Arrays.stream(context.fieldInfo.getXssSkip()).noneMatch(s -> s.equals(check))) {
                        return new String[]{"validation_xss"};
                    }
                }
            }

            return null;
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
            if (fieldInfo.getInputType() == InputType.file
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
            ItemData[] options = SystemController.getOptions(context.request, context.fieldInfo.getCode());
            if (options.length == 0) return null;

            if (isSingleSelect(context.fieldInfo)) {
                if (Arrays.stream(options).noneMatch(itemInfo -> itemInfo.getName().equals(context.parameterVal))) {
                    return optionsError(options);
                }
            } else if (isMultipleSelect(context.fieldInfo)) {
                String[] vals = context.parameterVal.split(context.fieldInfo.getSeparator());
                for (String v : vals) {
                    if (Arrays.stream(options).noneMatch(itemInfo -> itemInfo.getName().equals(v))) {
                        return optionsError(options);
                    }
                }
            }

            return null;
        }

        String[] optionsError(ItemData[] options) {
            String collect = Arrays.stream(options).map(ItemData::getName).collect(Collectors.joining(","));
            return new String[]{"validation_options", collect};
        }
    }

    static class range_datetime implements Validator {
        @Override
        public String[] validate(ValidationContext context) {
            if (InputType.range_datetime != context.fieldInfo.getInputType()) return null;

            try {
                for (String date : context.parameterVal.split(context.fieldInfo.getSeparator())) {
                    // 已转换在：qingzhou.console.controller.rest.ParameterFilter.datetime
                    new Date().setTime(Long.parseLong(date));
                }
                return null;
            } catch (Exception e) {
                return new String[]{"validation_datetime", DeployerConstants.DATETIME_FORMAT};
            }
        }
    }

    static class datetime implements Validator {
        @Override
        public String[] validate(ValidationContext context) {
            if (InputType.datetime != context.fieldInfo.getInputType()) return null;

            try {
                // 已转换在：qingzhou.console.controller.rest.ParameterFilter.datetime
                new Date().setTime(Long.parseLong(context.parameterVal));
                return null;
            } catch (Exception e) {
                return new String[]{"validation_datetime", DeployerConstants.DATETIME_FORMAT};
            }
        }
    }
}
