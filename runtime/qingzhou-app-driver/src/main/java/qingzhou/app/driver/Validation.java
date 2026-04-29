package qingzhou.app.driver;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import qingzhou.api.Constants;
import qingzhou.api.FieldType;
import qingzhou.api.InputType;
import qingzhou.api.type.Add;
import qingzhou.api.type.Update;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.registry.I18nService;

class Validation {
    // Error messages - i18n format: {"Chinese message", "en:English message"}
    private static final String[] MSG_REQUIRED = {"该字段是必填项", "en:This field is required"};
    private static final String[] MSG_NOT_NUMBER = {"须是有效的数字", "en:Must be a valid number"};
    private static final String[] MSG_LENGTH_BETWEEN = {"长度须在 %d 到 %d 个字符之间", "en:Length must be between %d and %d characters"};
    private static final String[] MSG_LENGTH_MIN = {"长度须至少 %d 个字符", "en:Length must be at least %d characters"};
    private static final String[] MSG_LENGTH_MAX = {"长度不能超过 %d 个字符", "en:Length cannot exceed %d characters"};

    private final I18nService i18nService;
    private final Map<Filter, Validator> validators = new LinkedHashMap<Filter, Validator>() {{
        put(context -> context.field.id, new PatternValidator("^[a-zA-Z0-9_-]{1,20}$",
                new String[]{"ID只能包含字母、数字、下划线和中划线，长度1-20位", "en:ID can only contain letters, numbers, underscores and hyphens, 1-20 characters"}));
        put(context -> context.field.input_type == InputType.number, new PatternValidator("^\\d+$",
                new String[]{"只能包含数字，且不能以0开头", "en:Can only contain digits and cannot start with 0"}));
        put(context -> context.field.input_type == InputType.decimal, new PatternValidator("^\\d+(\\.\\d+)?$",
                new String[]{"须是正整数或小数", "en:Must be a positive integer or decimal"}));
        put(context -> context.field.input_type == InputType.bool, new PatternValidator("^(true|false)$",
                new String[]{"只能是 true 或 false", "en:Must be either true or false"}));
        put(context -> context.field.min != Long.MIN_VALUE || context.field.max != Long.MAX_VALUE, new Range());
        put(context -> context.field.min_length != -1 || context.field.max_length == Integer.MAX_VALUE, new Length());
        put(context -> context.field.email, new PatternValidator("^[a-zA-Z0-9_+.-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$",
                new String[]{"须是合法的邮箱地址", "en:Must be a valid email address"}));
        put(context -> context.field.host, new Host());
        put(context -> context.field.port, new Port());
        put(context -> context.field.pattern != null, new CustomPattern());
    }};

    Validation(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    Map<String, java.util.List<String>> validate(ModelAction action, RequestImpl request) {
        // 确定要检验哪些字段
        Set<ModelField> validateFields = null;
        if (action.code.equals(Add.ACTION_CODE_ADD)) {
            validateFields = request.getCurrentModel().fields.stream()
                    .filter(field -> field.field_type == FieldType.FORM && field.add && !field.readonly)
                    .collect(Collectors.toSet());
        } else if (action.code.equals(Update.ACTION_CODE_UPDATE)) {
            validateFields = request.getCurrentModel().fields.stream()
                    .filter(field -> field.field_type == FieldType.FORM && field.update && !field.readonly)
                    .filter(field -> !field.id) // 更新的 id 不校验
                    .filter(field -> request.getParameter(field.code) != null) // rest 更新，可以指定要更新的字段，只校验传递过来的字段
                    .collect(Collectors.toSet());
        }
        if (validateFields == null || validateFields.isEmpty()) return null;

        // 开始校验
        Map<String, java.util.List<String>> validation = new HashMap<>();
        String langParameter = request.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);
        for (ModelField field : validateFields) {
            java.util.List<String> errors = new ArrayList<>();

            String parameter = request.getParameter(field.code);
            if (parameter == null || parameter.isEmpty()) {
                if (field.required) {
                    errors.add(i18nService.getI18n(MSG_REQUIRED, langParameter));
                }
                continue;
            }

            ValidationContext context = new ValidationContext(field, parameter, request, langParameter);
            for (Map.Entry<Filter, Validator> entry : validators.entrySet()) {
                Filter filter = entry.getKey();
                Validator validator = entry.getValue();
                if (filter.filter(context)) {
                    String error = validator.validate(context);
                    if (error != null) errors.add(error);
                }
            }

            if (!errors.isEmpty()) validation.put(field.code, errors);
        }
        return validation.isEmpty() ? null : validation;
    }

    interface Filter {
        boolean filter(ValidationContext context);
    }

    interface Validator {
        String validate(ValidationContext context);
    }

    static class ValidationContext {
        final ModelField field;
        final String parameter;
        final RequestImpl request;
        final String lang;

        ValidationContext(ModelField field, String parameter, RequestImpl request, String lang) {
            this.field = field;
            this.parameter = parameter;
            this.request = request;
            this.lang = lang;
        }
    }

    class PatternValidator implements Validator {
        final Pattern pattern;
        final String[] msgI18n;

        PatternValidator(String pattern, String[] msgI18n) {
            this.pattern = Pattern.compile(pattern);
            this.msgI18n = msgI18n;
        }

        @Override
        public String validate(ValidationContext context) {
            boolean matches = pattern.matcher(context.parameter).matches();
            return matches ? null : i18nService.getI18n(msgI18n, context.lang);
        }
    }

    class Range implements Validator {
        final String[] MSG_RANGE_BETWEEN = {"数值须在 %d 到 %d 之间", "en:Value must be between %d and %d"};
        final String[] MSG_RANGE_MIN = {"数值不能小于 %d", "en:Value cannot be less than %d"};
        final String[] MSG_RANGE_MAX = {"数值不能大于 %d", "en:Value cannot be greater than %d"};

        @Override
        public String validate(ValidationContext context) {
            try {
                double value = Double.parseDouble(context.parameter);
                boolean outOfRange = context.field.min != Long.MIN_VALUE && value < context.field.min;
                if (context.field.max != Long.MAX_VALUE && value > context.field.max) {
                    outOfRange = true;
                }
                if (outOfRange) {
                    if (context.field.min != Long.MIN_VALUE && context.field.max != Long.MAX_VALUE) {
                        return i18nService.getI18n(MSG_RANGE_BETWEEN, context.lang, context.field.min, context.field.max);
                    } else if (context.field.min != Long.MIN_VALUE) {
                        return i18nService.getI18n(MSG_RANGE_MIN, context.lang, context.field.min);
                    } else {
                        return i18nService.getI18n(MSG_RANGE_MAX, context.lang, context.field.max);
                    }
                }
                return null;
            } catch (NumberFormatException e) {
                return i18nService.getI18n(MSG_NOT_NUMBER, context.lang);
            }
        }
    }

    class Length implements Validator {
        @Override
        public String validate(ValidationContext context) {
            int length = context.parameter.length();
            boolean outOfRange = context.field.min_length != -1 && length < context.field.min_length;
            if (length > context.field.max_length) {
                outOfRange = true;
            }
            if (outOfRange) {
                if (context.field.min_length != -1 && context.field.max_length != Integer.MAX_VALUE) {
                    return i18nService.getI18n(MSG_LENGTH_BETWEEN, context.lang, context.field.min_length, context.field.max_length);
                } else if (context.field.min_length != -1) {
                    return i18nService.getI18n(MSG_LENGTH_MIN, context.lang, context.field.min_length);
                } else {
                    return i18nService.getI18n(MSG_LENGTH_MAX, context.lang, context.field.max_length);
                }
            }
            return null;
        }
    }

    class Host implements Validator {
        // Hostname pattern (RFC 1123) + IPv4 pattern
        final Pattern hostnamePattern = Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");
        final Pattern ipv4Pattern = Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

        final String[] MSG_HOST_INVALID = {"须是合法的主机名或 IP 地址", "en:Must be a valid hostname or IP address"};

        @Override
        public String validate(ValidationContext context) {
            boolean valid = hostnamePattern.matcher(context.parameter).matches()
                    || ipv4Pattern.matcher(context.parameter).matches();
            return valid ? null : i18nService.getI18n(MSG_HOST_INVALID, context.lang);
        }
    }

    class Port implements Validator {
        final String[] MSG_PORT_INTEGER = {"端口号须是有效的整数", "en:Port number must be a valid integer"};
        final String[] MSG_PORT_RANGE = new String[]{"端口号须在 1 到 65535 之间", "en:Port number must be between 1 and 65535"};

        @Override
        public String validate(ValidationContext context) {
            try {
                int port = Integer.parseInt(context.parameter);
                if (port < 1 || port > 65535) {
                    return i18nService.getI18n(MSG_PORT_RANGE, context.lang);
                }
                return null;
            } catch (NumberFormatException e) {
                return i18nService.getI18n(MSG_PORT_INTEGER, context.lang);
            }
        }
    }

    class CustomPattern implements Validator {
        final String[] MSG_PATTERN_INVALID = {"格式不正确，须匹配规则：%s", "en:Incorrect format, must match pattern: %s"};

        @Override
        public String validate(ValidationContext context) {
            String pattern = context.field.pattern;
            return context.parameter.matches(pattern) ? null : i18nService.getI18n(MSG_PATTERN_INVALID, context.lang, pattern);
        }
    }
}
