package qingzhou.app.driver.validation;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import qingzhou.api.FieldType;
import qingzhou.api.InputType;
import qingzhou.api.type.Add;
import qingzhou.api.type.Update;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;

public class Validation {
    private final java.util.List<Validator> validators = new ArrayList<>(Arrays.asList(
            new Id(), new Number(), new Decimal(), new Bool()
    ));

    public Map<String, java.util.List<String>> validate(ModelAction action, RequestImpl request) {
        // 确定要检验哪些字段
        Set<ModelField> validateFields = null;
        if (action.code.equals(Add.ACTION_CODE_ADD)) {
            validateFields = request.getCurrentModel().fields.stream()
                    .filter(field -> field.field_type == FieldType.FORM && field.add)
                    .collect(Collectors.toSet());
        } else if (action.code.equals(Update.ACTION_CODE_UPDATE)) {
            validateFields = request.getCurrentModel().fields.stream()
                    .filter(field -> field.field_type == FieldType.FORM && field.update)
                    .filter(field -> field.id) // 更新的 id 不校验
                    .filter(field -> request.getParameter(field.code) == null) // rest 更新，可以指定要更新的字段，而无需全量字段
                    .collect(Collectors.toSet());
        }
        if (validateFields == null || validateFields.isEmpty()) return null;

        // 开始校验
        Map<String, java.util.List<String>> validation = new HashMap<>();
        for (ModelField field : validateFields) {
            String parameter = request.getParameter(field.code);
            if (!field.required && (parameter == null || parameter.isEmpty())) continue; // 无需校验
            java.util.List<String> result = new ArrayList<>();
            ValidationContext context = new ValidationContext(field, parameter, request);
            for (Validator validator : validators) {
                String error = validator.validate(context);
                if (error != null) {
                    result.add(error);
                }
            }
            validation.put(field.code, result);
        }
        return validation;
    }

    public interface Validator {
        String validate(ValidationContext context);
    }

    static class Id implements Validator {
        static Pattern generalIdPattern = Pattern.compile("^[a-zA-Z0-9_-]{1,20}$");

        @Override
        public String validate(ValidationContext context) {
            if (!context.field.id) return null;

            boolean matches = generalIdPattern.matcher(context.parameter).matches();
            if (matches) {
                return "ID只能包含字母、数字、下划线和中划线，长度1-20位";
            } else {
                return null;
            }
        }
    }

    static class Number implements Validator {
        static Pattern pattern = Pattern.compile("^\\d+$");

        @Override
        public String validate(ValidationContext context) {
            if (context.field.input_type != InputType.number) return null;

            boolean valid = pattern.matcher(context.parameter).matches();
            return valid ? null : "只能包含数字，且不能以0开头";
        }
    }

    static class Decimal implements Validator {
        static Pattern pattern = Pattern.compile("^\\d+(\\.\\d+)?$");

        @Override
        public String validate(ValidationContext context) {
            if (context.field.input_type != InputType.decimal) return null;

            boolean valid = pattern.matcher(context.parameter).matches();
            return valid ? null : "须是正整数或小数";
        }
    }

    static class Bool implements Validator {

        @Override
        public String validate(ValidationContext context) {
            if (context.field.input_type != InputType.bool) return null;

            boolean valid = "true".equals(context.parameter) || "false".equals(context.parameter);
            return valid ? null : "只能是 true 或 false";
        }
    }
}
