package qingzhou.app;

import qingzhou.api.*;
import qingzhou.api.metadata.ModelFieldData;
import qingzhou.api.metadata.ModelManager;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Editable;
import qingzhou.api.type.Listable;
import qingzhou.framework.Constants;
import qingzhou.framework.util.IPUtil;
import qingzhou.framework.util.StringUtil;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class Validator {
    private final AppContext appContext;

    public Validator(AppContext appContext) {
        this.appContext = appContext;
        initI18n();
    }

    private void initI18n() {
        this.appContext.addI18n("validator.cannotWrite", new String[]{"不支持写入", "en:Cannot write"});
        this.appContext.addI18n("validator.cannotCreate", new String[]{"不支持创建", "en:Cannot be created"});
        this.appContext.addI18n("validator.require", new String[]{"不支持为空", "en:Cannot be empty"});
        this.appContext.addI18n("validator.dataInvalid", new String[]{"数据不合法，如不支持包含 %s 等特殊字符", "en:Data is invalid, for example, it cannot contain special characters such as %s"});
        this.appContext.addI18n("validator.idField", new String[]{"须以英文字母开头，支持英文字母、数字、下划线、中划线、冒号、.、#号、左斜杠",
                "en:Must start with an English letter, support English letters, numbers, underscores, underscores, colons, ., #, left slash"});
        this.appContext.addI18n("validator.optionRange", new String[]{"取值必须在%s中", "en:Value taken must be in %s"});
        this.appContext.addI18n("validator.valueBetween", new String[]{"取值必须介于%s - %s之间", "en:Value must be between %s and %s"});
        this.appContext.addI18n("validator.length.only", new String[]{"长度须是%s", "en:Length must be %s"});
        this.appContext.addI18n("validator.lengthBetween", new String[]{"长度必须介于%s - %s之间", "en:Length must be between %s and %s"});
        this.appContext.addI18n("validator.number", new String[]{"须是数字类型", "en:Must be a numeric type"});
        this.appContext.addI18n("validator.decimal", new String[]{"须是数字（含浮点）类型", "en:Must be a decimal (float included) type"});
        this.appContext.addI18n("validator.ip.illegal", new String[]{"非法的IP地址或域名", "en:Illegal IP address or host name"});
        this.appContext.addI18n("validator.larger.cannot", new String[]{"不支持大于%s", "en:Cannot be larger than %s"});
        this.appContext.addI18n("validator.date.larger.cannot", new String[]{"不能晚于%s", "en:No later than %s"});
        this.appContext.addI18n("validator.larger.minusOne.cannot", new String[]{"不支持大于 %s - 1", "en:Cannot be larger than %s - 1"});
        this.appContext.addI18n("validator.less.cannot", new String[]{"不支持小于%s", "en:Cannot be less than %s"});
        this.appContext.addI18n("validator.date.less.cannot", new String[]{"不能早于%s", "en:No earlier than %s"});
        this.appContext.addI18n("validator.less.current", new String[]{"不能早于当前时间", "en:Cannot be earlier than the current time"});
        this.appContext.addI18n("user.create.cannot.chinese", new String[]{"不支持包含中文字符", "en:Cannot contain Chinese characters"});
        this.appContext.addI18n("app.threadpool.canot.eq", new String[]{"和 %s 不支持配置为同一个", "en:Cannot be configured as the same as %s"});
        this.appContext.addI18n("validator.notSupportedCharacters", new String[]{"不支持包含字符：\"%s\"", "en:Cannot contain the characters: \"%s\""});
        this.appContext.addI18n("validator.pattern.not", new String[]{"须是一个合法的正则表达式", "en:Must be a valid regular expression"});
        this.appContext.addI18n("validator.UnsupportedCharset", new String[]{"不支持此编码", "en:Unsupported charset"});
        this.appContext.addI18n("validator.notfound", new String[]{"不存在", "en:Not found"});
        this.appContext.addI18n("validator.kv.require", new String[]{"变量名不支持为空", "en:Variable names cannot be empty"});
        this.appContext.addI18n("validator.kv.name.duplicate", new String[]{"变量名不能重复", "en:Variable names cannot be duplicated"});
        this.appContext.addI18n("validator.date-time.format", new String[]{"须使用 " + Constants.DATE_FORMAT + " 的时间格式", "en:Must use the time format " + Constants.DATE_FORMAT});
        this.appContext.addI18n("validator.xss", new String[]{"可能存在XSS风险或隐患", "en:There may be XSS risks or hidden dangers"});
        this.appContext.addI18n("validation.error.centralizedConsoleUrl", new String[]{"不支持的URL协议或内容格式", "en:Unsupported URL protocol or content format"});
    }

    private String getI18n(Request request, String key, Object... args) {
        return this.appContext.getAppMetadata().getI18n(request.getI18nLang(), key, args);
    }

    public boolean validate(Request request, Response response) throws Exception {
        if (!Createable.ACTION_NAME_ADD.equals(request.getActionName()) && !Editable.ACTION_NAME_UPDATE.equals(request.getActionName())) {
            return true;
        }

        Map<String, String> errorData = new HashMap<>();
        ModelManager modelManager = appContext.getAppMetadata().getModelManager();
        String[] allFieldNames = modelManager.getFieldNames(request.getModelName());
        for (String fieldName : allFieldNames) {
            String validate = validate(request, modelManager, request::getParameter, fieldName, request.getParameter(fieldName));
            if (StringUtil.notBlank(validate)) {
                errorData.put(fieldName, validate);
            }
        }

        if (!errorData.isEmpty()) {
            response.addData(errorData);
            response.setSuccess(false);
            return false;
        }

        return true;
    }

    private interface RequestParameter {
        String getParameter(String name);
    }

    private String validate(Request request, ModelManager modelManager, RequestParameter requestParameter, String fieldName, String newValue) throws Exception {
        // 上下文环境
        String modelName = request.getModelName();
        ModelFieldData modelField = modelManager.getModelField(modelName, fieldName);
        if (modelField == null) {
            return null;
        }

        try {
            if (!isEffective(fieldName0 -> String.valueOf(requestParameter.getParameter(fieldName0)), modelField.effectiveWhen().trim())) {// TODO: 不显示的属性不需要校验
                return null;
            }
        } catch (Exception e) {
            // 如果这里出错，多数数据类型错误，例如本该数字的，却传值为 字符串 等。
        }

        ModelBase tempModel = Controller.appManager.getApp(request.getAppName()).getModelInstance(request.getModelName());
        boolean isUpdate = Editable.ACTION_NAME_UPDATE.equals(request.getActionName());
        if (newValue == null) { // NOTE：不能使用 StringUtil.isBlank 来判断，空串 "" 表示有值，且与 null（无值） 是不同含义
            if (modelField.required()) {
                if (isUpdate && modelField.disableOnEdit()) { // for #NC-1624|创建时必填，编辑时允许为空。
                    return null;
                }
                if (modelField.disableOnCreate()) {
                    // 解决 Connector 同时有 compressibleMimeType的默认值和 required 标注，依然在此处被拦截问题
                    // 例如 Connector：证书路径虽然给了默认值，但如果用户清空输入框再提交就会报错，未拦截说必填项（根因在于 空串和null的判别），所以此处需要再次核验
                    String defaultValue = "";//getModelManager().getFieldValue(tempModel, fieldName); todo 获取默认值
                    if (StringUtil.isBlank(defaultValue)) {
                        return getI18n(request, "validator.require");
                    }
                }
            }
        } else {
            if (newValue.isEmpty()) {
                if (modelField.required()) { // 必填项，但页面的输入框为空的情况！！！例如安全域用户的用户名
                    boolean isUpdatingFile = isUpdate && modelField.type() == FieldType.file; // 上传应用，编辑时候
                    if (!isUpdatingFile) {
                        return getI18n(request, "validator.require");
                    }
                }
            }
            ValidatorContext vc = new ValidatorContext(newValue, modelField, fieldName, request, modelManager, requestParameter);

            Class<?>[] preValidatorClass = { // 有顺序要求
                    disableOnCreate.class,
                    options.class
            };
            String msg = validate(preValidatorClass, vc);
            if (msg != null) {
                return msg;
            }

            if (newValue.isEmpty()) {
                boolean isNumber = modelField.type() == FieldType.number || modelField.type() == FieldType.decimal;
                if (isNumber) {
                    return null;
                } else {
                    // sessionHa  tdg  密码字段有时候为空，有时候不为空，需要走自定义校验
                    return getI18n(request, tempModel.validate(request, fieldName));
                }
            }

            Class<?>[] validatorClass = { // 有顺序要求
                    isIdField.class,
                    chineseCharacterSupported.class,
                    notSupportedStrings.class,
                    notSupportedCharacters.class,
                    number.class,
                    decimal.class,
                    kv.class,
                    datetime.class,
                    length.class,
                    isPattern.class,
                    isPort.class,
                    isWildcardIp.class,
                    isIpOrHostname.class,
                    selectCharset.class,
                    noGreaterThan.class,
                    noGreaterThanMinusOne.class,
                    noLessThan.class,
                    cannotBeTheSameAs.class,
                    isURL.class,
                    safeCheck.class// 注意：让这个安全检查保持在最后，因为 xss 可能误杀，所以里面可能用到一些判定，比如 信任IP 的正则
            };

            msg = validate(validatorClass, vc);
            if (msg != null) {
                return msg;
            }
        }

        // 最后进行自定义校验
        return getI18n(request, tempModel.validate(request, fieldName));
    }

    private static String validate(Class<?>[] validatorClass, ValidatorContext vc) throws Exception {
        for (Class<?> vClass : validatorClass) {
            InternalValidator validator = (InternalValidator) vClass.newInstance();
            String msg = validator.validate(vc);
            if (msg != null) {
                return msg;
            }
        }
        return null;
    }

    public static boolean isMultiVal(FieldType fieldType) {
        return fieldType == FieldType.checkbox || fieldType == FieldType.sortableCheckbox || fieldType == FieldType.groupedMultiselect || fieldType == FieldType.multiselect;
    }

    public String dataInvalidMsg(Request request, char c) {
        return dataInvalidMsg(request, String.valueOf(c));
    }

    public String dataInvalidMsg(Request request, String s) {
        return String.format(getI18n(request, "validator.dataInvalid"), "\"" + s + "\"");
    }

    class ValidatorContext {
        final String modelName;
        final ModelFieldData modelField;
        final String newValue;
        final String fieldName;
        final Request request;
        final RequestParameter requestParameter;
        final ModelManager modelManager;

        private ValidatorContext(String newValue, ModelFieldData modelField, String fieldName, Request request, ModelManager modelManager, RequestParameter requestParameter) {
            this.modelName = request.getModelName();
            this.modelField = modelField;
            this.newValue = newValue;
            this.fieldName = fieldName;
            this.request = request;
            this.modelManager = modelManager;
            this.requestParameter = requestParameter;
        }

        boolean isAdd() {
            return Createable.ACTION_NAME_ADD.equals(request.getActionName());
        }

        boolean isUpdate() {
            return Editable.ACTION_NAME_UPDATE.equals(request.getActionName());
        }
    }

    private interface InternalValidator {
        String validate(ValidatorContext vc) throws Exception;
    }

    class isIdField implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 安全漏洞防护：jndi、lookup等参数不能包含“ldap://” “rmi://” 或者 $ 转义字符等。
            // 安全防护放在 useCustomizedValidator 之前
            if (vc.fieldName.equals(Listable.FIELD_NAME_ID)) {
                if (vc.isAdd()) {
                    OUT:
                    for (String risk : new String[]{"://", // 阻止 jndi、rmi 注入
                            "'", "`", " ", "\"",// 去掉了"$"，启动参数等需要设置 ${qingzhou.domain}
                            "<", ">"// 新加的，安全域用户为证书用户时候，输入<xxx>出错！！！
                    }) {
                        for (char skip : vc.modelField.skipCharacterCheck().toCharArray()) {
                            if (risk.length() == 1 && risk.toCharArray()[0] == skip) {
                                continue OUT;
                            }
                        }
                        if (vc.newValue.contains(risk)) {
                            return dataInvalidMsg(vc.request, risk);
                        }
                    }
                }
            }

            // id 字段，额外增加校验
            // 放在 useCustomizedValidator 之后，可 允许自定义id的校验
            if (vc.fieldName.equals(Listable.FIELD_NAME_ID)) {
                // 只能输入英文数字下划线和横线的正则表达式
                boolean matches = Pattern.compile("^[a-zA-Z0-9#_/.:-]+$").matcher(vc.newValue).find();
                if (!matches) {
                    return getI18n(vc.request, "validator.idField");
                }
            }

            return null;
        }
    }

    class disableOnCreate implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 字段不可创建
            if (vc.modelField.disableOnCreate() && vc.isAdd()) {
                if (StringUtil.notBlank(vc.newValue)) {
                    return getI18n(vc.request, "validator.cannotCreate");
                }
            }
            return null;
        }
    }

    class options implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 如果是范围限定的，首先验证是否在指定的范围内，一定要放在 空串 检测之前，否则 Boolean 传递 空串会被跳过验证
            Options optionManager = vc.modelManager.getOptions(vc.request, vc.modelName, vc.fieldName);
            if (optionManager != null) {
                List<Option> options = optionManager.options();
                List<String> keyList = new ArrayList<>();// set 没有顺序，会影响自动测试集合
                for (Option option : options) {
                    keyList.add(option.value());
                }

                String checkValue = vc.newValue;
                if (vc.modelField.type() == FieldType.bool) {
                    checkValue = checkValue.toLowerCase(); // 支持大小的boolean值的后续校验
                }
                if (!isMultiVal(vc.modelField.type())) {
                    if (StringUtil.notBlank(checkValue) && !keyList.contains(checkValue)) {
                        if (keyList.isEmpty()) {
                            if (StringUtil.isBlank(checkValue)) {
                                return getI18n(vc.request, "validator.require");
                            } else {
                                return getI18n(vc.request, "validator.notfound");
                            }
                        } else {
                            return String.format(getI18n(vc.request, "validator.optionRange"), Arrays.toString(keyList.toArray()));
                        }
                    }
                } else {
                    for (String data : checkValue.split(Constants.DATA_SEPARATOR)) { //  例如：角色的权限，经过 ActionContext的getRequestParameter转换后包含逗号
                        boolean multiselect = vc.modelField.type() == FieldType.groupedMultiselect;
                        boolean containsSP = data.contains("/") && !data.startsWith("/") && !data.endsWith("/");
                        boolean isMultiselect = (multiselect && !containsSP);
                        if (isMultiselect || !keyList.contains(data)) {
                            // 多选框如果传递空串，则标识全不选，如果为null则不改变
                            boolean hasNotSelect = data.isEmpty();
                            if (!hasNotSelect) {
                                if (keyList.isEmpty()) {
                                    // 解决提示信息不友好：取值必须在[]中.
                                    return getI18n(vc.request, "validator.notfound");
                                } else {
                                    if (multiselect) {
                                        keyList.removeIf(s -> !s.contains("/"));// 去掉 父类 的全选引用，这个只对浏览器有用
                                    }
                                    if (!keyList.contains(data)) {
                                        if (keyList.size() <= 10) {
                                            return String.format(getI18n(vc.request, "validator.optionRange"), Arrays.toString(keyList.toArray()));
                                        } else {
                                            return String.format(getI18n(vc.request, "validator.optionRange"), keyList.get(0) + "," + keyList.get(1) + "," + keyList.get(2) + ",...");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return null;
        }
    }

    class chineseCharacterSupported implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 是否支持中文字符
            if (vc.modelField.noSupportZHChar()) {
                if (StringUtil.containsZHChar(vc.newValue)) {
                    return getI18n(vc.request, "user.create.cannot.chinese");
                }
            }

            return null;
        }
    }

    class notSupportedStrings implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.notSupportedStrings().length > 0) {
                String[] valueStrs = isMultiVal(vc.modelField.type()) ? vc.newValue.split(Constants.DATA_SEPARATOR) : new String[]{vc.newValue};
                for (String e : vc.modelField.notSupportedStrings()) {
                    for (String valueStr : valueStrs) {
                        if (valueStr.toLowerCase().contains(e.toLowerCase())) {
                            return String.format(getI18n(vc.request, "validator.notSupportedCharacters"), e);
                        }
                    }
                }
            }

            return null;
        }
    }

    class notSupportedCharacters implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (!vc.modelField.notSupportedCharacters().isEmpty()) { // 不能用 isBlank，因为可能只是配置了一个 空白 符
                String[] valueStrs = isMultiVal(vc.modelField.type()) ? vc.newValue.split(Constants.DATA_SEPARATOR) : new String[]{vc.newValue};
                for (char e : vc.modelField.notSupportedCharacters().toCharArray()) {
                    for (String valueStr : valueStrs) {
                        if (valueStr.indexOf(e) > -1) {
                            return String.format(getI18n(vc.request, "validator.notSupportedCharacters"), e);
                        }
                    }
                }
            }
            return null;
        }
    }

    class number implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 数字和数字区间校验范围校验
            if (vc.modelField.type() == FieldType.number) {
                try {
                    long guessNumber = Long.parseLong(vc.newValue);
                    if (guessNumber < vc.modelField.min() || guessNumber > vc.modelField.max()) {
                        return String.format(getI18n(vc.request, "validator.valueBetween"), vc.modelField.min(), vc.modelField.max());
                    }
                } catch (NumberFormatException e) {
                    return getI18n(vc.request, "validator.number");
                }
            }

            return null;
        }
    }

    class decimal implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 数字和数字区间校验范围校验
            if (vc.modelField.type() == FieldType.decimal) {
                try {
                    BigDecimal decimalValue = new BigDecimal(vc.newValue);
                    if (decimalValue.doubleValue() < vc.modelField.min() || decimalValue.doubleValue() > vc.modelField.max()) {
                        return String.format(getI18n(vc.request, "validator.valueBetween"), vc.modelField.min(), vc.modelField.max());
                    }
                } catch (Exception e) {
                    return getI18n(vc.request, "validator.decimal");
                }
            }

            return null;
        }
    }

    class kv implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 校验key-value键值对的一般格式
            if (vc.modelField.type() == FieldType.kv) {
                if (StringUtil.notBlank(vc.newValue)) {
                    String[] arr = vc.newValue.split(Constants.DATA_SEPARATOR);
                    Set<String> keys = new HashSet<>();
                    for (String s : arr) {
                        int i = s.indexOf("=");
                        if (i < 0) {
                            keys.add(s);
                        } else {
                            String k = s.substring(0, i);
                            if (StringUtil.isBlank(k)) {
                                return getI18n(vc.request, "validator.kv.require");
                            }
                            keys.add(k);
                        }
                    }
                    if (keys.size() < arr.length) {
                        return getI18n(vc.request, "validator.kv.name.duplicate");
                    }
                }
            }

            return null;
        }
    }

    class datetime implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.type() == FieldType.datetime) {
                String modelName = vc.modelName;

                if (StringUtil.notBlank(vc.newValue)) {
                    DateFormat dateFormat;
                    Date thisDateTime;
                    try {
                        dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
                        thisDateTime = dateFormat.parse(vc.newValue);
                    } catch (ParseException e) {
                        return getI18n(vc.request, "validator.date-time.format");
                    }

                    String noGreaterThan = vc.modelField.noGreaterOrEqualThanDate().trim();
                    if (!noGreaterThan.isEmpty()) {
                        try {
                            String thanObj = vc.requestParameter.getParameter(noGreaterThan);
                            Date otherDateTime = dateFormat.parse(thanObj);
                            if (!thisDateTime.before(otherDateTime)) {
                                String msg = getI18n(vc.request, "validator.date.larger.cannot");
                                return String.format(msg, getI18n(vc.request, "model.field." + modelName + "." + noGreaterThan));
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    String noLessThan = vc.modelField.noLessOrEqualThanDate().trim();
                    if (!noLessThan.isEmpty()) {
                        try {
                            String thanObj = vc.requestParameter.getParameter(noGreaterThan);
                            if (StringUtil.notBlank(thanObj)) {
                                Date otherDateTime = dateFormat.parse(thanObj);
                                if (!thisDateTime.after(otherDateTime)) {
                                    String msg = getI18n(vc.request, "validator.date.less.cannot");
                                    return String.format(msg, getI18n(vc.request, "model.field." + modelName + "." + noLessThan));
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    if (vc.isAdd() // 对于 list 类型的模块，创建时候可以验证，但是编辑时候可能已经过了当时的时间，不必再校验，否则引起校验不通过
                            || (vc.isUpdate()) // 对于 service 类型的，则需要每次验证
                    ) {
                        if (vc.modelField.noLessThanCurrentTime()) {
                            if (!thisDateTime.after(new Date())) {
                                return getI18n(vc.request, "validator.less.current");
                            }
                        }
                    }

                }
            }

            return null;
        }
    }

    class length implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 字符串长度校验 1
            if (vc.modelField.maxLength() < 1) {
                if (StringUtil.notBlank(vc.newValue)) {
                    return getI18n(vc.request, "validator.cannotWrite");
                }
            }

            // 字符串长度校验 2
            String[] valueStrs = isMultiVal(vc.modelField.type()) ? vc.newValue.split(Constants.DATA_SEPARATOR) : new String[]{vc.newValue};
            for (String v : valueStrs) {
                if (v.length() < vc.modelField.minLength() || v.length() > vc.modelField.maxLength()) {
                    if (vc.modelField.minLength() == vc.modelField.maxLength()) {
                        return String.format(getI18n(vc.request, "validator.length.only"), vc.modelField.minLength(), vc.modelField.maxLength());
                    } else {
                        return String.format(getI18n(vc.request, "validator.lengthBetween"), vc.modelField.minLength(), vc.modelField.maxLength());
                    }
                }
            }

            return null;
        }
    }

    class isPattern implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.isPattern()) {
                if (StringUtil.notBlank(vc.newValue)) {
                    try {
                        Pattern.compile(vc.newValue);// for #ITAIT-4107，不能按 Constans.Data_Separator 拆分
                    } catch (Exception ex) {
                        return getI18n(vc.request, "validator.pattern.not");
                    }
                }
            }
            return null;
        }
    }

    class isPort implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.isPort()) {
                try {
                    long guessNumber = Long.parseLong(vc.newValue);
                    int min = 1;
                    int max = 65535;
                    if (guessNumber < min || guessNumber > max) {
                        return String.format(getI18n(vc.request, "validator.valueBetween"), min, max);
                    }
                } catch (NumberFormatException e) {
                    return getI18n(vc.request, "validator.number");
                }
            }
            return null;
        }
    }

    class isWildcardIp implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.isWildcardIp()) {
                if (!"*".equals(vc.newValue)) {
                    try {
                        Pattern.compile(vc.newValue);// for #ITAIT-4107，不能按 Constans.Data_Separator 拆分
                    } catch (Exception ex) {
                        return getI18n(vc.request, "validator.pattern.not");
                    }
                }
            }
            return null;
        }
    }

    class isIpOrHostname implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.isIpOrHostname()) {
                if (!IPUtil.isIpOrHost(vc.newValue)) {
                    return getI18n(vc.request, "validator.ip.illegal");
                }
            }
            return null;
        }
    }

    class selectCharset implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.type() == FieldType.selectCharset) {
                if (StringUtil.notBlank(vc.newValue)) {
                    try {
                        Charset.forName(vc.newValue);
                    } catch (Exception ignored) {
                        return getI18n(vc.request, "validator.UnsupportedCharset");
                    }
                }
            }
            return null;
        }
    }

    class noGreaterThan implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            String noGreaterThan = vc.modelField.noGreaterThan().trim();
            if (!noGreaterThan.isEmpty()) {
                String value = vc.requestParameter.getParameter(noGreaterThan);
                Number arg = Long.valueOf(value);
                if (Long.parseLong(vc.newValue) > 0 && arg.longValue() > 0) {// 0 有特殊含义（如禁用此功能、永远生效等），不参与比较
                    if (Long.parseLong(vc.newValue) > arg.longValue()) {
                        String msg = getI18n(vc.request, "validator.larger.cannot");
                        return String.format(msg, getI18n(vc.request, "model.field." + vc.modelName + "." + noGreaterThan));
                    }
                }
            }
            return null;
        }
    }

    class noGreaterThanMinusOne implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            String noGreaterThanMinusOne = vc.modelField.noGreaterThanMinusOne().trim();
            if (!noGreaterThanMinusOne.isEmpty()) {
                String value = vc.requestParameter.getParameter(noGreaterThanMinusOne);
                Number arg = Long.valueOf(value);
                if (Long.parseLong(vc.newValue) > 0 && arg.longValue() > 0) {// 0 有特殊含义（如禁用此功能、永远生效等），不参与比较
                    if (Long.parseLong(vc.newValue) > arg.longValue() - 1) {
                        String msg = getI18n(vc.request, "validator.larger.minusOne.cannot");
                        return String.format(msg, getI18n(vc.request, "model.field." + vc.modelName + "." + noGreaterThanMinusOne));
                    }
                }
            }
            return null;
        }
    }

    class noLessThan implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            String noLessThan = vc.modelField.noLessThan().trim();
            if (!noLessThan.isEmpty()) {
                String value = vc.requestParameter.getParameter(noLessThan);
                Number arg = Long.valueOf(value);
                if (Long.parseLong(vc.newValue) > 0 && arg.longValue() > 0) { // 0 有特殊含义（如禁用此功能、永远生效等），不参与比较
                    if (Long.parseLong(vc.newValue) < arg.longValue()) {
                        String msg = getI18n(vc.request, "validator.less.cannot");
                        return String.format(msg, getI18n(vc.request, "model.field." + vc.modelName + "." + noLessThan));
                    }
                }
            }

            return null;
        }
    }

    class cannotBeTheSameAs implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            String cannotBeTheSameAs = vc.modelField.cannotBeTheSameAs();
            if (StringUtil.notBlank(cannotBeTheSameAs) && StringUtil.notBlank(vc.newValue)) {
                for (String field : cannotBeTheSameAs.split(Constants.DATA_SEPARATOR)) {
                    String fieldValue = vc.request.getParameter(field);
                    if (fieldValue == null && vc.isUpdate()) {
                        fieldValue = vc.requestParameter.getParameter(field);
                    }
                    if (Objects.equals(fieldValue, vc.newValue)) {
                        String p1 = getI18n(vc.request, "model.field." + vc.modelName + "." + field);
                        return String.format(getI18n(vc.request, "app.threadpool.canot.eq"), p1);
                    }
                }
            }

            return null;
        }
    }

    class isURL implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (!vc.modelField.isURL()) return null;

            if (StringUtil.isBlank(vc.newValue)) return null;

            String useDefaultProtocol = "http://";
            if (vc.newValue.contains("://")) {
                useDefaultProtocol = null;
                if (!vc.newValue.startsWith("http://") && !vc.newValue.startsWith("https://")) {
                    return getI18n(vc.request, "validation.error.centralizedConsoleUrl");
                }
            }

            try {
                String protocol = StringUtil.notBlank(useDefaultProtocol) ? useDefaultProtocol : "";
                String url = protocol + vc.newValue;
                new URL(url);
                return null;
            } catch (MalformedURLException e) {
                return getI18n(vc.request, "validation.error.centralizedConsoleUrl");
            }

        }
    }

    class safeCheck implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.skipSafeCheck()) {
                return null;
            }
            if (vc.modelField.type() == FieldType.password) {
                return null;
            }
            if (StringUtil.notBlank(vc.newValue)) {
                boolean ok = true;
                for (int i = 0; i < vc.newValue.length(); i++) {
                    if (!vc.modelField.skipCharacterCheck().contains(vc.newValue.substring(i, i + 1))) {
                        ok = false;
                        break;
                    }
                }
                if (ok) { // 全部都是跳过的，则不必再进行后续的检查了
                    return null;
                }
            }

            // for #ITAIT-1446
            // 安全检查：#ITAIT-4940 #NC-1705 等// 有自己的校验方式
            boolean skipInjectionRiskCheck = vc.modelField.isWildcardIp()
                    || vc.modelField.isIpOrHostname()
                    /*|| vc.modelField.isCron()*/
                    || vc.modelField.isPattern();
            if (!skipInjectionRiskCheck) {
                String risk;
                if (StringUtil.notBlank(risk = new SafeCheckerUtil().hasCommandInjectionRiskWithSkip(vc.newValue, vc.modelField.skipCharacterCheck()))) {
                    return dataInvalidMsg(vc.request, risk);
                }
            }

            // XML 非法字符 "<"和"&"，可能潜在风险，如必须使用，可使用 useCustomizedValidator=true
            char[] forbiddenXmlStrs = {'<', '&', '\"'};
            OUT:
            for (char f : forbiddenXmlStrs) {
                for (char skip : vc.modelField.skipCharacterCheck().toCharArray()) {
                    if (f == skip) {
                        continue OUT;
                    }
                }
                for (char c : vc.newValue.toCharArray()) {
                    if (f == c) {
                        return dataInvalidMsg(vc.request, f);
                    }
                }
            }

            if (vc.modelField.isPattern() // 自身进行了标准正则校验
                    || vc.modelField.isWildcardIp() // 自身进行了标准正则校验
                    || vc.modelField.checkXssLevel1()
                    || vc.modelField.isIpOrHostname()
            ) { // 正则会被匹配 xss，有自己的规则，所以不必走 xss 校验
                // xss 漏洞
                if (!new SafeCheckerUtil().checkXssLevel1(vc.newValue)) {
                    return getI18n(vc.request, "validator.xss");
                }
            } else {
                // xss 漏洞
                if (new SafeCheckerUtil().checkIsXSS(vc.newValue)) {
                    return getI18n(vc.request, "validator.xss");
                }
            }

            return null;
        }
    }

    public static boolean isEffective(FieldValueRetriever retriever, String effectiveWhen) throws Exception {
        if (StringUtil.isBlank(effectiveWhen)) {
            return true;
        }

        AndOrQueue queue = null;
        String[] split;
        if ((split = effectiveWhen.split("&")).length > 1) {
            queue = new AndOrQueue(true);
        } else if ((split = effectiveWhen.split("\\|")).length > 1) {
            queue = new AndOrQueue(false);
        }
        if (queue == null) {
            if (split.length > 0) {
                queue = new AndOrQueue(true);
            }
        }
        if (queue == null) {
            return true;
        }

        String notEqStr = "!=";
        String eqStr = "=";
        for (String s : split) {
            int notEq = s.indexOf(notEqStr);
            if (notEq > 1) {
                String f = s.substring(0, notEq);
                String v = s.substring(notEq + notEqStr.length());
                queue.addComparator(new Comparator(false, retriever.getFieldValue(f), v));
                continue;
            }
            int eq = s.indexOf(eqStr);
            if (eq > 1) {
                String f = s.substring(0, eq);
                String v = s.substring(eq + eqStr.length());
                queue.addComparator(new Comparator(true, retriever.getFieldValue(f), v));
            }
        }

        return queue.compare();
    }

    private static final class AndOrQueue {
        final boolean andOr;
        final List<Comparator> comparators = new ArrayList<>();

        AndOrQueue(boolean andOr) {
            this.andOr = andOr;
        }

        void addComparator(Comparator comparator) {
            comparators.add(comparator);
        }

        boolean compare() {
            if (andOr) {
                for (Comparator c : comparators) {
                    if (!c.compare()) {
                        return false;
                    }
                }
                return true;
            } else {
                for (Comparator c : comparators) {
                    if (c.compare()) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private static final class Comparator {
        final boolean eqOrNot;
        final String v1;
        final String v2;

        Comparator(boolean eqOrNot, String v1, String v2) {
            this.eqOrNot = eqOrNot;
            this.v1 = v1;
            this.v2 = v2;
        }

        boolean compare() {
            String vv1 = v1;
            String vv2 = v2;
            if (vv1 != null) {
                vv1 = vv1.toLowerCase();
            }
            if (vv2 != null) {
                vv2 = vv2.toLowerCase();
            }
            return eqOrNot == Objects.equals(vv1, vv2);
        }
    }

    public interface FieldValueRetriever {
        String getFieldValue(String fieldName) throws Exception;
    }


    public class SafeCheckerUtil {
        private final String[] CommandInjectionRisk = new String[]{"`", "$", ";", "&", "|", "{", "}", "(", ")", "[", "]", "../", "..\\", "*", "%", "~", "^", "!"};// windows 路径会存在空格

        public String hasCommandInjectionRiskWithSkip(String arg, String skips) {
            for (String f : CommandInjectionRisk) { // 命令行执行注入漏洞
                if (skips != null) {
                    if (skips.contains(f)) {
                        continue;
                    }
                }
                if (arg.contains(f)) {
                    return f;
                }
            }

            return null;
        }

        private final Pattern SCRIPT_PATTERN = Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE);

        public boolean checkIsXSS(String check) {
            return !checkXssOk(check);
        }

        // Level1 的检查，可以让大多数的正则（允许使用括号、中括号等）通过
        public boolean checkXssLevel1(String check) {
            if (StringUtil.isBlank(check)) {
                return true;
            }

            //判断url是否带有<>
            String resultUrl = check.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            if (!resultUrl.equals(check)) {
                return false;
            }

            resultUrl = resultUrl.replaceAll("eval\\((.*)\\)", "");
            if (!resultUrl.equals(check)) {
                return false;
            }

            //onmouseover漏洞
            //List<String> onXXEventPrefixList = new ArrayList<String>();
            //onXXEventPrefixList.addAll(Arrays.asList("%20", "&nbsp;", "\"", "'", "/", "\\+"));
            resultUrl = SCRIPT_PATTERN.matcher(resultUrl).replaceAll("");
            if (!resultUrl.equals(check)) {
                return false;
            }

            // 拦截这种攻击方式：payload:'onmousemove         =confirm(1)//
            return (!resultUrl.contains("'") && !resultUrl.contains("\""))
                    || resultUrl.indexOf(")") <= resultUrl.indexOf("(");
        }

        public boolean checkXssOk(String check) {
            if (StringUtil.isBlank(check)) return true;

            if (!checkXssLevel1(check)) {
                return false;
            }

            String resultUrl = check.replaceAll("\\(", "&#40").replaceAll("\\)", "&#41");
            if (!resultUrl.equals(check)) {
                return false;
            }


            resultUrl = resultUrl.replaceAll("\\[", "&#91").replaceAll("\\]", "&#93");
            return resultUrl.equals(check);
        }
    }

}
