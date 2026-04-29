package qingzhou.app.driver.validation;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import qingzhou.api.FieldType;
import qingzhou.api.InputType;
import qingzhou.api.Lang;
import qingzhou.api.type.Add;
import qingzhou.api.type.Update;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.registry.I18nService;

public class Validation {
    private I18nService i18nService;

    // Error messages - i18n format: {"Chinese message", "en:English message"}
    private static final String[] MSG_ID_INVALID = {"ID只能包含字母、数字、下划线和中划线，长度1-20位", "en:ID can only contain letters, numbers, underscores and hyphens, 1-20 characters"};
    private static final String[] MSG_NUMBER_INVALID = {"只能包含数字，且不能以0开头", "en:Can only contain digits and cannot start with 0"};
    private static final String[] MSG_DECIMAL_INVALID = {"须是正整数或小数", "en:Must be a positive integer or decimal"};
    private static final String[] MSG_BOOL_INVALID = {"只能是 true 或 false", "en:Must be either true or false"};
    private static final String[] MSG_REQUIRED = {"该字段是必填项", "en:This field is required"};
    private static final String[] MSG_RANGE_BETWEEN = {"数值须在 %d 到 %d 之间", "en:Value must be between %d and %d"};
    private static final String[] MSG_RANGE_MIN = {"数值不能小于 %d", "en:Value cannot be less than %d"};
    private static final String[] MSG_RANGE_MAX = {"数值不能大于 %d", "en:Value cannot be greater than %d"};
    private static final String[] MSG_NOT_NUMBER = {"须是有效的数字", "en:Must be a valid number"};
    private static final String[] MSG_LENGTH_BETWEEN = {"长度须在 %d 到 %d 个字符之间", "en:Length must be between %d and %d characters"};
    private static final String[] MSG_LENGTH_MIN = {"长度须至少 %d 个字符", "en:Length must be at least %d characters"};
    private static final String[] MSG_LENGTH_MAX = {"长度不能超过 %d 个字符", "en:Length cannot exceed %d characters"};
    private static final String[] MSG_EMAIL_INVALID = {"须是合法的邮箱地址", "en:Must be a valid email address"};
    private static final String[] MSG_HOST_INVALID = {"须是合法的主机名或 IP 地址", "en:Must be a valid hostname or IP address"};
    private static final String[] MSG_PORT_RANGE = {"端口号须在 1 到 65535 之间", "en:Port number must be between 1 and 65535"};
    private static final String[] MSG_PORT_INTEGER = {"端口号须是有效的整数", "en:Port number must be a valid integer"};
    private static final String[] MSG_PATTERN_INVALID = {"格式不正确，须匹配规则：%s", "en:Incorrect format, must match pattern: %s"};

    private final java.util.List<Validator> validators = new ArrayList<>(Arrays.asList(
            new Id(), new Number(), new Decimal(), new Bool(),
            new Range(), new Length(), new Email(), new Host(), new Port(), new CustomPattern()
    ));

    private Lang getLang(RequestImpl request) {
        String langStr = request.getParameter("lang");
        if (langStr == null || langStr.isEmpty()) {
            return Lang.zh;
        }
        try {
            return Lang.valueOf(langStr);
        } catch (IllegalArgumentException e) {
            return Lang.zh;
        }
    }

    public void setI18nService(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    public Map<String, java.util.List<String>> validate(ModelAction action, RequestImpl request) {
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
        Lang lang = getLang(request);
        for (ModelField field : validateFields) {
            String parameter = request.getParameter(field.code);
            if (!field.required && (parameter == null || parameter.isEmpty())) continue; // 无需校验

            java.util.List<String> result = new ArrayList<>();
            // 必填项检查
            if (field.required && (parameter == null || parameter.isEmpty())) {
                result.add(i18nService.getI18n(MSG_REQUIRED, lang));
            }

            if (parameter != null && !parameter.isEmpty()) {
                ValidationContext context = new ValidationContext(field, parameter, request);
                for (Validator validator : validators) {
                    String error = validator.validate(context, lang, i18nService);
                    if (error != null) {
                        result.add(error);
                    }
                }
            }

            if (!result.isEmpty()) {
                validation.put(field.code, result);
            }
        }
        return validation.isEmpty() ? null : validation;
    }

    public interface Validator {
        String validate(ValidationContext context, Lang lang, I18nService i18nService);
    }

    static class Id implements Validator {
        static final Pattern generalIdPattern = Pattern.compile("^[a-zA-Z0-9_-]{1,20}$");

        @Override
        public String validate(ValidationContext context, Lang lang, I18nService i18nService) {
            if (!context.field.id) return null;
            if (context.parameter == null || context.parameter.isEmpty()) return null;

            boolean matches = generalIdPattern.matcher(context.parameter).matches();
            if (!matches) {
                return i18nService.getI18n(MSG_ID_INVALID, lang);
            } else {
                return null;
            }
        }
    }

    static class Number implements Validator {
        static final Pattern pattern = Pattern.compile("^\\d+$");

        @Override
        public String validate(ValidationContext context, Lang lang, I18nService i18nService) {
            if (context.field.input_type != InputType.number) return null;
            if (context.parameter == null || context.parameter.isEmpty()) return null;

            boolean valid = pattern.matcher(context.parameter).matches();
            return valid ? null : i18nService.getI18n(MSG_NUMBER_INVALID, lang);
        }
    }

    static class Decimal implements Validator {
        static final Pattern pattern = Pattern.compile("^\\d+(\\.\\d+)?$");

        @Override
        public String validate(ValidationContext context, Lang lang, I18nService i18nService) {
            if (context.field.input_type != InputType.decimal) return null;
            if (context.parameter == null || context.parameter.isEmpty()) return null;

            boolean valid = pattern.matcher(context.parameter).matches();
            return valid ? null : i18nService.getI18n(MSG_DECIMAL_INVALID, lang);
        }
    }

    static class Bool implements Validator {

        @Override
        public String validate(ValidationContext context, Lang lang, I18nService i18nService) {
            if (context.field.input_type != InputType.bool) return null;
            if (context.parameter == null || context.parameter.isEmpty()) return null;

            boolean valid = "true".equals(context.parameter) || "false".equals(context.parameter);
            return valid ? null : i18nService.getI18n(MSG_BOOL_INVALID, lang);
        }
    }

    static class Range implements Validator {
        @Override
        public String validate(ValidationContext context, Lang lang, I18nService i18nService) {
            if (context.parameter == null || context.parameter.isEmpty()) return null;

            // Check for min and max on numeric fields
            long min = context.field.min;
            long max = context.field.max;
            if (min == Long.MIN_VALUE && max == Long.MAX_VALUE) return null;

            try {
                double value = Double.parseDouble(context.parameter);
                boolean outOfRange = false;
                if (min != Long.MIN_VALUE && value < min) {
                    outOfRange = true;
                }
                if (max != Long.MAX_VALUE && value > max) {
                    outOfRange = true;
                }
                if (outOfRange) {
                    if (min != Long.MIN_VALUE && max != Long.MAX_VALUE) {
                        return i18nService.getI18n(MSG_RANGE_BETWEEN, lang, min, max);
                    } else if (min != Long.MIN_VALUE) {
                        return i18nService.getI18n(MSG_RANGE_MIN, lang, min);
                    } else {
                        return i18nService.getI18n(MSG_RANGE_MAX, lang, max);
                    }
                }
                return null;
            } catch (NumberFormatException e) {
                return i18nService.getI18n(MSG_NOT_NUMBER, lang);
            }
        }
    }

    static class Length implements Validator {
        @Override
        public String validate(ValidationContext context, Lang lang, I18nService i18nService) {
            if (context.parameter == null || context.parameter.isEmpty()) return null;

            int min = context.field.min_length;
            int max = context.field.max_length;
            if (min == -1 && max == Integer.MAX_VALUE) return null;

            int length = context.parameter.length();
            boolean outOfRange = false;
            if (min != -1 && length < min) {
                outOfRange = true;
            }
            if (max != Integer.MAX_VALUE && length > max) {
                outOfRange = true;
            }
            if (outOfRange) {
                if (min != -1 && max != Integer.MAX_VALUE) {
                    return i18nService.getI18n(MSG_LENGTH_BETWEEN, lang, min, max);
                } else if (min != -1) {
                    return i18nService.getI18n(MSG_LENGTH_MIN, lang, min);
                } else {
                    return i18nService.getI18n(MSG_LENGTH_MAX, lang, max);
                }
            }
            return null;
        }
    }

    static class Email implements Validator {
        static final Pattern pattern = Pattern.compile("^[a-zA-Z0-9_+.-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$");

        @Override
        public String validate(ValidationContext context, Lang lang, I18nService i18nService) {
            if (!context.field.email) return null;
            if (context.parameter == null || context.parameter.isEmpty()) return null;

            boolean valid = pattern.matcher(context.parameter).matches();
            return valid ? null : i18nService.getI18n(MSG_EMAIL_INVALID, lang);
        }
    }

    static class Host implements Validator {
        // Hostname pattern (RFC 1123) + IPv4 pattern
        static final Pattern hostnamePattern = Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");
        static final Pattern ipv4Pattern = Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

        @Override
        public String validate(ValidationContext context, Lang lang, I18nService i18nService) {
            if (!context.field.host) return null;
            if (context.parameter == null || context.parameter.isEmpty()) return null;

            boolean valid = hostnamePattern.matcher(context.parameter).matches()
                    || ipv4Pattern.matcher(context.parameter).matches();
            return valid ? null : i18nService.getI18n(MSG_HOST_INVALID, lang);
        }
    }

    static class Port implements Validator {
        @Override
        public String validate(ValidationContext context, Lang lang, I18nService i18nService) {
            if (!context.field.port) return null;
            if (context.parameter == null || context.parameter.isEmpty()) return null;

            try {
                int port = Integer.parseInt(context.parameter);
                if (port < 1 || port > 65535) {
                    return i18nService.getI18n(MSG_PORT_RANGE, lang);
                }
                return null;
            } catch (NumberFormatException e) {
                return i18nService.getI18n(MSG_PORT_INTEGER, lang);
            }
        }
    }

    static class CustomPattern implements Validator {
        private final Map<String, java.util.regex.Pattern> compiledPatterns = new HashMap<>();

        @Override
        public String validate(ValidationContext context, Lang lang, I18nService i18nService) {
            String pattern = context.field.pattern;
            if (pattern == null || pattern.isEmpty()) return null;
            if (context.parameter == null || context.parameter.isEmpty()) return null;

            java.util.regex.Pattern compiled = compiledPatterns.get(pattern);
            if (compiled == null) {
                try {
                    compiled = java.util.regex.Pattern.compile(pattern);
                    compiledPatterns.put(pattern, compiled);
                } catch (Exception e) {
                    return null; // Invalid regex pattern, skip validation
                }
            }

            boolean valid = compiled.matcher(context.parameter).matches();
            return valid ? null : i18nService.getI18n(MSG_PATTERN_INVALID, lang, pattern);
        }
    }
}
