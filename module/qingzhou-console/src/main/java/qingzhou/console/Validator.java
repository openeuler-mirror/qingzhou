package qingzhou.console;

import qingzhou.api.AppContextHelper;
import qingzhou.api.console.ConsoleContext;
import qingzhou.api.console.FieldType;
import qingzhou.api.console.ModelField;
import qingzhou.api.console.ModelManager;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.api.console.model.AddModel;
import qingzhou.api.console.model.EditModel;
import qingzhou.api.console.model.ListModel;
import qingzhou.api.console.model.ModelBase;
import qingzhou.api.console.option.Option;
import qingzhou.api.console.option.OptionManager;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.util.Constants;
import qingzhou.console.util.IPUtil;
import qingzhou.console.util.ObjectUtil;
import qingzhou.console.util.SafeCheckerUtil;
import qingzhou.console.util.StringUtil;

import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class Validator {
    static {
        ConsoleContext consoleContext = ConsoleWarHelper.getMasterAppConsoleContext();
        if (consoleContext != null) {
            consoleContext.addI18N("validator.arg.memory.invalid", new String[]{"参数不合法，此参数应以数字加单位(m/M、g/G)构成", "en:The parameter is not legal, this parameter should be composed of numbers plus units (m/M, g/G)."});
            consoleContext.addI18N("validator.arg.memory.union.invalid", new String[]{"联合校验参数【%s】不合法，该参数应以数字加单位(m/M、g/G)构成", "en:The joint verification parameter [%s] is illegal, and the parameter should be composed of numbers plus units (m/m, g/g)."});
            consoleContext.addI18N("validator.cannotWrite", new String[]{"不支持写入", "en:Cannot write"});
            consoleContext.addI18N("validator.cannotEdit", new String[]{"不支持编辑", "en:Cannot be edited"});
            consoleContext.addI18N("validator.cannotCreate", new String[]{"不支持创建", "en:Cannot be created"});
            consoleContext.addI18N("validator.require", new String[]{"不支持为空", "en:Cannot be empty"});
            consoleContext.addI18N("validator.dataInvalid", new String[]{"数据不合法，如不支持包含 %s 等特殊字符", "en:Data is invalid, for example, it cannot contain special characters such as %s"});
            consoleContext.addI18N("validator.idField", new String[]{"须以英文字母开头，支持英文字母、数字、下划线、中划线、冒号、.、#号、左斜杠",
                    "en:Must start with an English letter, support English letters, numbers, underscores, underscores, colons, ., #, left slash"});
            consoleContext.addI18N("validator.optionRange", new String[]{"取值必须在%s中", "en:Value taken must be in %s"});
            consoleContext.addI18N("validator.valueBetween", new String[]{"取值必须介于%s - %s之间", "en:Value must be between %s and %s"});
            consoleContext.addI18N("validator.length.only", new String[]{"长度须是%s", "en:Length must be %s"});
            consoleContext.addI18N("validator.lengthBetween", new String[]{"长度必须介于%s - %s之间", "en:Length must be between %s and %s"});
            consoleContext.addI18N("validator.number", new String[]{"须是数字类型", "en:Must be a numeric type"});
            consoleContext.addI18N("validator.decimal", new String[]{"须是数字（含浮点）类型", "en:Must be a decimal (float included) type"});
            consoleContext.addI18N("validator.exist", new String[]{"已存在", "en:Already exists"});
            consoleContext.addI18N("validator.ip.illegal", new String[]{"非法的IP地址或域名", "en:Illegal IP address or host name"});
            consoleContext.addI18N("validator.larger.cannot", new String[]{"不支持大于%s", "en:Cannot be larger than %s"});
            consoleContext.addI18N("validator.date.larger.cannot", new String[]{"不能晚于%s", "en:No later than %s"});
            consoleContext.addI18N("validator.larger.minusOne.cannot", new String[]{"不支持大于 %s - 1", "en:Cannot be larger than %s - 1"});
            consoleContext.addI18N("validator.less.cannot", new String[]{"不支持小于%s", "en:Cannot be less than %s"});
            consoleContext.addI18N("validator.date.less.cannot", new String[]{"不能早于%s", "en:No earlier than %s"});
            consoleContext.addI18N("validator.less.current", new String[]{"不能早于当前时间", "en:Cannot be earlier than the current time"});
            consoleContext.addI18N("user.create.cannot.chinese", new String[]{"不支持包含中文字符", "en:Cannot contain Chinese characters"});
            consoleContext.addI18N("app.threadpool.canot.eq", new String[]{"和 %s 不支持配置为同一个", "en:Cannot be configured as the same as %s"});
            consoleContext.addI18N("validator.notSupportedCharacters", new String[]{"不支持包含字符：\"%s\"", "en:Cannot contain the characters: \"%s\""});
            consoleContext.addI18N("validator.pattern.not", new String[]{"须是一个合法的正则表达式", "en:Must be a valid regular expression"});
            consoleContext.addI18N("validator.UnsupportedCharset", new String[]{"不支持此编码", "en:Unsupported charset"});
            consoleContext.addI18N("validator.notfound", new String[]{"不存在", "en:Not found"});
            consoleContext.addI18N("validator.kv.require", new String[]{"变量名不支持为空", "en:Variable names cannot be empty"});
            consoleContext.addI18N("validator.kv.name.duplicate", new String[]{"变量名不能重复", "en:Variable names cannot be duplicated"});
            consoleContext.addI18N("validator.date-time.format", new String[]{"须使用 " + Constants.DATE_FORMAT + " 的时间格式", "en:Must use the time format " + Constants.DATE_FORMAT});
            consoleContext.addI18N("validator.xss", new String[]{"可能存在XSS风险或隐患", "en:There may be XSS risks or hidden dangers"});
            consoleContext.addI18N("validation.error.cron", new String[]{"内容格式有误，不是一个合法的 Cron 表达式",
                    "en:The content is malformed and is not a valid cron expression"});
            consoleContext.addI18N("validation.error.centralizedConsoleUrl", new String[]{"不支持的URL协议或内容格式", "en:Unsupported URL protocol or content format"});
            consoleContext.addI18N("validator.hasRefModel", new String[]{"%s 正在被其它模块使用，不支持对其进行该操作，使用模块：%s", "en:%s is being used by other modules, this operation cannot be performed on it, using module: %s"});
        }
    }

    public static boolean validate(Request request, Response response, ModelManager modelManager) throws Exception {
        if (!AddModel.ACTION_NAME_ADD.equals(request.getActionName()) && !AddModel.ACTION_NAME_UPDATE.equals(request.getActionName())) {
            return true;
        }

        Map<String, String> errorData = new HashMap<>();
        String[] allFieldNames = modelManager.getAllFieldNames(request.getModelName());
        boolean singleFieldValidation = Validator.isSingleFieldValidation(request);
        String singleField = request.getParameter(Constants.SINGLE_FIELD_VALIDATE_PARAM);
        for (String fieldName : allFieldNames) {
            if (singleFieldValidation && !fieldName.equals(singleField)) {
                continue;
            }
            String validate = validate((RequestImpl) request, modelManager, fieldName, request.getParameter(fieldName));
            if (StringUtil.notBlank(validate)) {
                errorData.put(fieldName, validate);
            }
        }

        if (!errorData.isEmpty()) {
            response.errorData().addData(errorData);
            response.setSuccess(false);
            return false;
        }

        return !singleFieldValidation;// 单字段的校验不需要走后续的持久化
    }

    private static String validate(RequestImpl request, ModelManager modelManager, String fieldName, String newValue) throws Exception {
        // 上下文环境
        String modelName = request.getModelName();
        ModelField modelField = modelManager.getModelField(modelName, fieldName);
        if (modelField == null) {
            return null;
        }

        ModelBase tempModel = modelManager.getModelInstance(modelName);

        try {
            Map<String, String> dataMap = ((EditModel) tempModel).prepareParameters(request);
            ObjectUtil.setObjectValues(tempModel, dataMap);
            if (!isEffective(fieldName0 -> String.valueOf(tempModel.getClass().getField(fieldName)
                    .get(tempModel)), modelField.effectiveWhen().trim())) {// TODO: 不显示的属性不需要校验
                return null;
            }

        } catch (Exception e) {
            // 如果这里出错，多数数据类型错误，例如本该数字的，却传值为 字符串 等。
        }

        boolean isUpdate = EditModel.ACTION_NAME_UPDATE.equals(request.getActionName());
        if (newValue == null) { // NOTE：不能使用 StringUtil.isBlank 来判断，空串 "" 表示有值，且与 null（无值） 是不同含义
            if (modelField.required()) {
                if (isUpdate && !modelField.effectiveOnEdit()) { // for #NC-1624|创建时必填，编辑时允许为空。
                    return null;
                }
                if (modelField.effectiveOnCreate()) {
                    // 解决 Connector 同时有 compressibleMimeType的默认值和 required 标注，依然在此处被拦截问题
                    // 例如 Connector：证书路径虽然给了默认值，但如果用户清空输入框再提交就会报错，未拦截说必填项（根因在于 空串和null的判别），所以此处需要再次核验
                    String defaultValue = "";//getModelManager().getFieldValue(tempModel, fieldName); todo 获取默认值
                    if (StringUtil.isBlank(defaultValue)) {
                        return AppContextHelper.getAppContext().getConsoleContext().getI18N("validator.require");
                    }
                }
            }
        } else {
            ValidatorContext vc = new ValidatorContext(newValue, modelField, fieldName, request, modelManager, tempModel);

            Class<?>[] preValidatorClass = { // 有顺序要求
                    readOnly.class,
                    effectiveOnCreate.class,
                    options.class
            };
            String msg = validate(preValidatorClass, vc);
            if (msg != null) {
                return msg;
            }

            if (newValue.isEmpty()) {
                boolean isNumber = modelField.type() == FieldType.number || modelField.type() == FieldType.decimal;
                if (isNumber) {
                    request.removeParameter(fieldName);// 不是必填项，但传递了一个空串，视为 没有传递，即视为 null
                    return null;
                } else {
                    // sessionHa  tdg  密码字段有时候为空，有时候不为空，需要走自定义校验
                    return tempModel.validate(request, fieldName);
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
        return tempModel.validate(request, fieldName);
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

    private static boolean isSingleFieldValidation(Request request) {
        return request.getParameter(Constants.SINGLE_FIELD_VALIDATE_PARAM) != null;
    }

    public static boolean isMultiVal(FieldType fieldType) {
        return fieldType == FieldType.checkbox || fieldType == FieldType.sortableCheckbox || fieldType == FieldType.groupedMultiselect || fieldType == FieldType.multiselect;
    }

    public static String dataInvalidMsg(char c) {
        return dataInvalidMsg(String.valueOf(c));
    }

    public static String dataInvalidMsg(String s) {
        return String.format(AppContextHelper.getAppContext().getConsoleContext().getI18N("validator.dataInvalid"), "\"" + s + "\"");
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

    private static final class AndOrQueue {
        final boolean andOr;
        final List<Validator.Comparator> comparators = new ArrayList<>();

        AndOrQueue(boolean andOr) {
            this.andOr = andOr;
        }

        void addComparator(Validator.Comparator comparator) {
            comparators.add(comparator);
        }

        boolean compare() {
            if (andOr) {
                for (Validator.Comparator c : comparators) {
                    if (!c.compare()) {
                        return false;
                    }
                }
                return true;
            } else {
                for (Validator.Comparator c : comparators) {
                    if (c.compare()) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public interface FieldValueRetriever<T> {
        String getFieldValue(String fieldName) throws Exception;
    }

    static class ValidatorContext {
        final String modelName;
        final ModelField modelField;
        final String newValue;
        final String fieldName;
        final Request request;
        final ModelBase tempModel;
        final ModelManager modelManager;
        final ConsoleContext context;

        private ValidatorContext(String newValue, ModelField modelField, String fieldName, Request request, ModelManager modelManager, ModelBase tempModel) {
            this.modelName = request.getModelName();
            this.modelField = modelField;
            this.newValue = newValue;
            this.fieldName = fieldName;
            this.request = request;
            this.modelManager = modelManager;
            this.tempModel = tempModel;
            this.context = AppContextHelper.getAppContext().getConsoleContext();
        }

        boolean isAdd() {
            return AddModel.ACTION_NAME_ADD.equals(request.getActionName());
        }

        boolean isUpdate() {
            return EditModel.ACTION_NAME_UPDATE.equals(request.getActionName());
        }
    }

    private interface InternalValidator {
        String validate(ValidatorContext vc) throws Exception;
    }

    static class isIdField implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 安全漏洞防护：jndi、lookup等参数不能包含“ldap://” “rmi://” 或者 $ 转义字符等。
            // 安全防护放在 useCustomizedValidator 之前
            if (vc.fieldName.equals(ListModel.FIELD_NAME_ID)) {
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
                            return dataInvalidMsg(risk);
                        }
                    }
                }
            }

            // id 字段，额外增加校验
            // 放在 useCustomizedValidator 之后，可 允许自定义id的校验
            if (vc.fieldName.equals(ListModel.FIELD_NAME_ID) && !vc.modelField.skipIdFormat()) {
                // 只能输入英文数字下划线和横线的正则表达式
                boolean matches = Pattern.compile("^[a-zA-Z0-9#_/.:-]+$").matcher(vc.newValue).find();
                if (!matches) {
                    return vc.context.getI18N("validator.idField");
                }
            }

            return null;
        }
    }


    static class readOnly implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 字段不可写（创建和编辑）
            /*if (vc.modelManager.isFieldReadOnly(vc.actionContext, vc.tempModel, vc.fieldName)) {
                if (!vc.newValue.equals(vc.oldValue)) {
                    return vc.context.getI18N( "validator.cannotWrite");
                }
            }*/ // TODO
            return null;
        }
    }

    static class effectiveOnCreate implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 字段不可创建
            if (!vc.modelField.effectiveOnCreate() && vc.isAdd()) {
                if (StringUtil.notBlank(vc.newValue)) {
                    return vc.context.getI18N("validator.cannotCreate");
                }
            }
            return null;
        }
    }

    static class options implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 如果是范围限定的，首先验证是否在指定的范围内，一定要放在 空串 检测之前，否则 Boolean 传递 空串会被跳过验证
            OptionManager optionManager = vc.tempModel.fieldOptions(vc.request, vc.fieldName);
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
                                return vc.context.getI18N("validator.require");
                            } else {
                                return vc.context.getI18N("validator.notfound");
                            }
                        } else {
                            return String.format(vc.context.getI18N("validator.optionRange"), Arrays.toString(keyList.toArray()));
                        }
                    }
                } else {
                    for (String data : checkValue.split(Constants.DATA_SEPARATOR)) { //  例如：角色的权限，经过 ActionContext的getRequestParameter转换后包含逗号
                        boolean multiselect = vc.modelField.type() == FieldType.groupedMultiselect;
                        boolean containsSP = data.contains("/") && !data.startsWith("/") && !data.endsWith("/");
                        boolean isMultiselect = (multiselect && !containsSP);
                        if (isMultiselect || !keyList.contains(data)) {
                            // 多选框如果传递空串，则标识全不选，如果为null则不改变
                            boolean hasNotSelect = "".equals(data);
                            if (!hasNotSelect) {
                                if (keyList.isEmpty()) {
                                    // 解决提示信息不友好：取值必须在[]中.
                                    return vc.context.getI18N("validator.notfound");
                                } else {
                                    if (multiselect) {
                                        keyList.removeIf(s -> !s.contains("/"));// 去掉 父类 的全选引用，这个只对浏览器有用
                                    }
                                    if (!keyList.contains(data)) {
                                        if (keyList.size() <= 10) {
                                            return String.format(vc.context.getI18N("validator.optionRange"), Arrays.toString(keyList.toArray()));
                                        } else {
                                            return String.format(vc.context.getI18N("validator.optionRange"), keyList.get(0) + "," + keyList.get(1) + "," + keyList.get(2) + ",...");
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

    static class chineseCharacterSupported implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 是否支持中文字符
            if (vc.modelField.noSupportZHChar()) {
                if (StringUtil.containsZHChar(vc.newValue)) {
                    return vc.context.getI18N("user.create.cannot.chinese");
                }
            }

            return null;
        }
    }

    static class notSupportedStrings implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.notSupportedStrings().length > 0) {
                String[] valueStrs = isMultiVal(vc.modelField.type()) ? vc.newValue.split(Constants.DATA_SEPARATOR) : new String[]{vc.newValue};
                for (String e : vc.modelField.notSupportedStrings()) {
                    for (String valueStr : valueStrs) {
                        if (valueStr.toLowerCase().contains(e.toLowerCase())) {
                            return String.format(vc.context.getI18N("validator.notSupportedCharacters"), e);
                        }
                    }
                }
            }

            return null;
        }
    }

    static class notSupportedCharacters implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.notSupportedCharacters().length() > 0) { // 不能用 isBlank，因为可能只是配置了一个 空白 符
                String[] valueStrs = isMultiVal(vc.modelField.type()) ? vc.newValue.split(Constants.DATA_SEPARATOR) : new String[]{vc.newValue};
                for (char e : vc.modelField.notSupportedCharacters().toCharArray()) {
                    for (String valueStr : valueStrs) {
                        if (valueStr.indexOf(e) > -1) {
                            return String.format(vc.context.getI18N("validator.notSupportedCharacters"), e);
                        }
                    }
                }
            }
            return null;
        }
    }

    static class number implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 数字和数字区间校验范围校验
            if (vc.modelField.type() == FieldType.number) {
                try {
                    long guessNumber = Long.parseLong(vc.newValue);
                    if (guessNumber < vc.modelField.min() || guessNumber > vc.modelField.max()) {
                        return String.format(vc.context.getI18N("validator.valueBetween"), vc.modelField.min(), vc.modelField.max());
                    }
                } catch (Exception e) {
                    return vc.context.getI18N("validator.number");
                }
            }

            return null;
        }
    }

    static class decimal implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 数字和数字区间校验范围校验
            if (vc.modelField.type() == FieldType.decimal) {
                try {
                    BigDecimal decimalValue = new BigDecimal(vc.newValue);
                    if (decimalValue.doubleValue() < vc.modelField.min() || decimalValue.doubleValue() > vc.modelField.max()) {
                        return String.format(vc.context.getI18N("validator.valueBetween"), vc.modelField.min(), vc.modelField.max());
                    }
                } catch (Exception e) {
                    return vc.context.getI18N("validator.decimal");
                }
            }

            return null;
        }
    }

    static class kv implements InternalValidator {

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
                                return vc.context.getI18N("validator.kv.require");
                            }
                            keys.add(k);
                        }
                    }
                    if (keys.size() < arr.length) {
                        return vc.context.getI18N("validator.kv.name.duplicate");
                    }
                }
            }

            return null;
        }
    }

    static class datetime implements InternalValidator {

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
                    } catch (Exception e) {
                        return vc.context.getI18N("validator.date-time.format");
                    }

                    String noGreaterThan = vc.modelField.noGreaterOrEqualThanDate().trim();
                    if (!noGreaterThan.isEmpty()) {
                        try {
                            String thanObj = String.valueOf(ObjectUtil.getObjectValue(vc.tempModel, noGreaterThan));
                            Date otherDateTime = dateFormat.parse(thanObj);
                            if (!thisDateTime.before(otherDateTime)) {
                                String msg = vc.context.getI18N("validator.date.larger.cannot");
                                return String.format(msg, vc.context.getI18N("model.field." + modelName + "." + noGreaterThan));
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    String noLessThan = vc.modelField.noLessOrEqualThanDate().trim();
                    if (!noLessThan.isEmpty()) {
                        try {
                            String thanObj = String.valueOf(ObjectUtil.getObjectValue(vc.tempModel, noGreaterThan));
                            if (StringUtil.notBlank(thanObj)) {
                                Date otherDateTime = dateFormat.parse(thanObj);
                                if (!thisDateTime.after(otherDateTime)) {
                                    String msg = vc.context.getI18N("validator.date.less.cannot");
                                    return String.format(msg, vc.context.getI18N("model.field." + modelName + "." + noLessThan));
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
                                return vc.context.getI18N("validator.less.current");
                            }
                        }
                    }

                }
            }

            return null;
        }
    }

    static class length implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            // 字符串长度校验 1
            if (vc.modelField.maxLength() < 1) {
                if (StringUtil.notBlank(vc.newValue)) {
                    return vc.context.getI18N("validator.cannotWrite");
                }
            }

            // 字符串长度校验 2
            String[] valueStrs = isMultiVal(vc.modelField.type()) ? vc.newValue.split(Constants.DATA_SEPARATOR) : new String[]{vc.newValue};
            for (String v : valueStrs) {
                if (v.length() < vc.modelField.minLength() || v.length() > vc.modelField.maxLength()) {
                    if (vc.modelField.minLength() == vc.modelField.maxLength()) {
                        return String.format(vc.context.getI18N("validator.length.only"), vc.modelField.minLength(), vc.modelField.maxLength());
                    } else {
                        return String.format(vc.context.getI18N("validator.lengthBetween"), vc.modelField.minLength(), vc.modelField.maxLength());
                    }
                }
            }

            return null;
        }
    }

    static class isPattern implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.isPattern()) {
                if (StringUtil.notBlank(vc.newValue)) {
                    try {
                        Pattern.compile(vc.newValue);// for #ITAIT-4107，不能按 Constans.Data_Separator 拆分
                    } catch (Exception ex) {
                        return vc.context.getI18N("validator.pattern.not");
                    }
                }
            }
            return null;
        }
    }

    static class isPort implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.isPort()) {
                try {
                    long guessNumber = Long.parseLong(vc.newValue);
                    int min = 1;
                    int max = 65535;
                    if (guessNumber < min || guessNumber > max) {
                        return String.format(vc.context.getI18N("validator.valueBetween"), min, max);
                    }
                } catch (Exception e) {
                    return vc.context.getI18N("validator.number");
                }
            }
            return null;
        }
    }

    static class isWildcardIp implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.isWildcardIp()) {
                if (!"*".equals(vc.newValue)) {
                    try {
                        Pattern.compile(vc.newValue);// for #ITAIT-4107，不能按 Constans.Data_Separator 拆分
                    } catch (Exception ex) {
                        return vc.context.getI18N("validator.pattern.not");
                    }
                }
            }
            return null;
        }
    }

    static class isIpOrHostname implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.isIpOrHostname()) {
                if (!IPUtil.isIpOrHost(vc.newValue)) {
                    return vc.context.getI18N("validator.ip.illegal");
                }
            }
            return null;
        }
    }

    static class selectCharset implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (vc.modelField.type() == FieldType.selectCharset) {
                if (StringUtil.notBlank(vc.newValue)) {
                    try {
                        Charset.forName(vc.newValue);
                    } catch (Exception ignored) {
                        return vc.context.getI18N("validator.UnsupportedCharset");
                    }
                }
            }
            return null;
        }
    }

    static class noGreaterThan implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            String noGreaterThan = vc.modelField.noGreaterThan().trim();
            if (!noGreaterThan.isEmpty() && vc.request.getParameter(Constants.SINGLE_FIELD_VALIDATE_PARAM) == null) {
                String value = String.valueOf(ObjectUtil.getObjectValue(vc.tempModel, noGreaterThan));
                Number arg = Long.valueOf(value);
                if (Long.parseLong(vc.newValue) > 0 && arg.longValue() > 0) {// 0 有特殊含义（如禁用此功能、永远生效等），不参与比较
                    if (Long.parseLong(vc.newValue) > arg.longValue()) {
                        String msg = vc.context.getI18N("validator.larger.cannot");
                        return String.format(msg, vc.context.getI18N("model.field." + vc.modelName + "." + noGreaterThan));
                    }
                }
            }
            return null;
        }
    }

    static class noGreaterThanMinusOne implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            String noGreaterThanMinusOne = vc.modelField.noGreaterThanMinusOne().trim();
            if (!noGreaterThanMinusOne.isEmpty() && vc.request.getParameter(Constants.SINGLE_FIELD_VALIDATE_PARAM) == null) {
                String value = String.valueOf(ObjectUtil.getObjectValue(vc.tempModel, noGreaterThanMinusOne));
                Number arg = Long.valueOf(value);
                if (Long.parseLong(vc.newValue) > 0 && arg.longValue() > 0) {// 0 有特殊含义（如禁用此功能、永远生效等），不参与比较
                    if (Long.parseLong(vc.newValue) > arg.longValue() - 1) {
                        String msg = vc.context.getI18N("validator.larger.minusOne.cannot");
                        return String.format(msg, vc.context.getI18N("model.field." + vc.modelName + "." + noGreaterThanMinusOne));
                    }
                }
            }
            return null;
        }
    }

    static class noLessThan implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            String noLessThan = vc.modelField.noLessThan().trim();
            if (!noLessThan.isEmpty() && vc.request.getParameter(Constants.SINGLE_FIELD_VALIDATE_PARAM) == null) {
                String value = String.valueOf(ObjectUtil.getObjectValue(vc.tempModel, noLessThan));
                Number arg = Long.valueOf(value);
                if (Long.parseLong(vc.newValue) > 0 && arg.longValue() > 0) { // 0 有特殊含义（如禁用此功能、永远生效等），不参与比较
                    if (Long.parseLong(vc.newValue) < arg.longValue()) {
                        String msg = vc.context.getI18N("validator.less.cannot");
                        return String.format(msg, vc.context.getI18N("model.field." + vc.modelName + "." + noLessThan));
                    }
                }
            }

            return null;
        }
    }

    static class cannotBeTheSameAs implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            String cannotBeTheSameAs = vc.modelField.cannotBeTheSameAs();
            if (StringUtil.notBlank(cannotBeTheSameAs) && StringUtil.notBlank(vc.newValue) && vc.request.getParameter(Constants.SINGLE_FIELD_VALIDATE_PARAM) == null) {
                for (String field : cannotBeTheSameAs.split(Constants.DATA_SEPARATOR)) {
                    String fieldValue = vc.request.getParameter(field);
                    if (fieldValue == null && vc.isUpdate()) {
                        fieldValue = String.valueOf(ObjectUtil.getObjectValue(vc.tempModel, field));
                    }
                    if (Objects.equals(fieldValue, vc.newValue)) {
                        String p1 = vc.context.getI18N("model.field." + vc.modelName + "." + field);
                        return String.format(vc.context.getI18N("app.threadpool.canot.eq"), p1);
                    }
                }
            }

            return null;
        }
    }

    static class isURL implements InternalValidator {

        @Override
        public String validate(ValidatorContext vc) throws Exception {
            if (!vc.modelField.isURL()) return null;

            if (StringUtil.isBlank(vc.newValue)) return null;

            String useDefaultProtocol = "http://";
            if (vc.newValue.contains("://")) {
                useDefaultProtocol = null;
                if (!vc.newValue.startsWith("http://") && !vc.newValue.startsWith("https://")) {
                    return vc.context.getI18N("validation.error.centralizedConsoleUrl");
                }
            }

            try {
                String protocol = StringUtil.notBlank(useDefaultProtocol) ? useDefaultProtocol : "";
                String url = protocol + vc.newValue;
                new URL(url);
                return null;
            } catch (Exception e) {
                return vc.context.getI18N("validation.error.centralizedConsoleUrl");
            }

        }
    }

    static class safeCheck implements InternalValidator {

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
                if (StringUtil.notBlank(risk = SafeCheckerUtil.hasCommandInjectionRiskWithSkip(vc.newValue, vc.modelField.skipCharacterCheck()))) {
                    return dataInvalidMsg(risk);
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
                        return dataInvalidMsg(f);
                    }
                }
            }

            if (vc.modelField.isPattern() // 自身进行了标准正则校验
                    || vc.modelField.isWildcardIp() // 自身进行了标准正则校验
                    || vc.modelField.checkXssLevel1()
                    || vc.modelField.isIpOrHostname()
            ) { // 正则会被匹配 xss，有自己的规则，所以不必走 xss 校验
                // xss 漏洞
                if (!SafeCheckerUtil.checkXssLevel1(vc.newValue)) {
                    return vc.context.getI18N("validator.xss");
                }
            } else {
                // xss 漏洞
                if (SafeCheckerUtil.checkIsXSS(vc.newValue)) {
                    return vc.context.getI18N("validator.xss");
                }
            }

            return null;
        }
    }

}
