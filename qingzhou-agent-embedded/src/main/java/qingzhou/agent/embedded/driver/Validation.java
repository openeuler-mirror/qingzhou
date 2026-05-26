package qingzhou.agent.embedded.driver;

import java.util.*;

import qingzhou.agent.embedded.i18n.I18nService;
import qingzhou.api.InputType;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;

class Validation {
    private final I18nService i18nService;
    private static final String[] MSG_REQUIRED = {"{0}是必填项", "en:{0} is required"};
    private static final String[] MSG_NUMBER = {"{0}请输入数字", "en:{0} please enter a number"};
    private static final String[] MSG_MIN = {"{0}值不能小于{1}", "en:{0} value cannot be less than {1}"};
    private static final String[] MSG_MAX = {"{0}值不能大于{1}", "en:{0} value cannot be greater than {1}"};
    private static final String[] MSG_MIN_LENGTH = {"{0}长度不能小于{1}", "en:{0} length cannot be less than {1}"};
    private static final String[] MSG_MAX_LENGTH = {"{0}长度不能大于{1}", "en:{0} length cannot be greater than {1}"};
    private static final String[] MSG_EMAIL = {"{0}不是有效的Email地址", "en:{0} is not a valid email address"};
    private static final String[] MSG_HOST = {"{0}不是有效的主机名", "en:{0} is not a valid hostname"};
    private static final String[] MSG_PORT = {"{0}不是有效的端口号", "en:{0} is not a valid port number"};
    private static final String[] MSG_PATTERN = {"{0}格式不匹配", "en:{0} format does not match"};

    Validation(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    Map<String, List<String>> validate(ModelAction action, RequestImpl request) {
        String lang = request.getParameter(qingzhou.api.Constants.REQUEST_PARAMETER_NAME_LANG);
        List<ModelField> modelFields = request.getCurrentModel().fields;
        Map<String, List<String>> errors = new HashMap<>();
        for (ModelField field : modelFields) {
            if (!(action.add || action.update)) continue;
            if ((action.add && !field.add) || (action.update && !field.update)) continue;
            if (field.readonly) continue;

            String value = request.getParameter(field.code);
            boolean empty = isBlank(value);
            if (empty) {
                if (field.required) {
                    errors.computeIfAbsent(field.code, k -> new ArrayList<>())
                            .add(i18nService.getI18n(MSG_REQUIRED, lang, i18nService.getI18n(field.name, lang)));
                }
                continue;
            }

            if (field.input_type == InputType.number || field.input_type == InputType.decimal) {
                try {
                    if (field.input_type == InputType.number) {
                        Long.parseLong(value);
                    } else {
                        Double.parseDouble(value);
                    }
                } catch (NumberFormatException e) {
                    errors.computeIfAbsent(field.code, k -> new ArrayList<>())
                            .add(i18nService.getI18n(MSG_NUMBER, lang, i18nService.getI18n(field.name, lang)));
                }
            }

            if (field.input_type == InputType.number || field.input_type == InputType.decimal) {
                if (field.min != 0) {
                    try {
                        if (Double.parseDouble(value) < field.min) {
                            errors.computeIfAbsent(field.code, k -> new ArrayList<>())
                                    .add(i18nService.getI18n(MSG_MIN, lang,
                                            i18nService.getI18n(field.name, lang),
                                            String.valueOf(field.min)));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (field.max != 0) {
                    try {
                        if (Double.parseDouble(value) > field.max) {
                            errors.computeIfAbsent(field.code, k -> new ArrayList<>())
                                    .add(i18nService.getI18n(MSG_MAX, lang,
                                            i18nService.getI18n(field.name, lang),
                                            String.valueOf(field.max)));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (field.min_length > 0 && value.length() < field.min_length) {
                errors.computeIfAbsent(field.code, k -> new ArrayList<>())
                        .add(i18nService.getI18n(MSG_MIN_LENGTH, lang,
                                i18nService.getI18n(field.name, lang),
                                String.valueOf(field.min_length)));
            }
            if (field.max_length > -1 && value.length() > field.max_length) {
                errors.computeIfAbsent(field.code, k -> new ArrayList<>())
                        .add(i18nService.getI18n(MSG_MAX_LENGTH, lang,
                                i18nService.getI18n(field.name, lang),
                                String.valueOf(field.max_length)));
            }
            if (field.email && !isValidEmail(value)) {
                errors.computeIfAbsent(field.code, k -> new ArrayList<>())
                        .add(i18nService.getI18n(MSG_EMAIL, lang, i18nService.getI18n(field.name, lang)));
            }
            if (field.host && !isValidHost(value)) {
                errors.computeIfAbsent(field.code, k -> new ArrayList<>())
                        .add(i18nService.getI18n(MSG_HOST, lang, i18nService.getI18n(field.name, lang)));
            }
            if (field.port && !isValidPort(value)) {
                errors.computeIfAbsent(field.code, k -> new ArrayList<>())
                        .add(i18nService.getI18n(MSG_PORT, lang, i18nService.getI18n(field.name, lang)));
            }
            if (field.pattern != null && !field.pattern.isEmpty() && !value.matches(field.pattern)) {
                errors.computeIfAbsent(field.code, k -> new ArrayList<>())
                        .add(i18nService.getI18n(MSG_PATTERN, lang, i18nService.getI18n(field.name, lang)));
            }
        }
        return errors;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isValidEmail(String value) {
        return value != null && value.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    private boolean isValidHost(String value) {
        return value != null && value.matches("^[\\w.-]+$");
    }

    private boolean isValidPort(String value) {
        try {
            int port = Integer.parseInt(value);
            return port > 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}