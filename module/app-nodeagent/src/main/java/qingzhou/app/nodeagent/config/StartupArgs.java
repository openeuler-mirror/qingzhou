package qingzhou.app.nodeagent.config;

import qingzhou.api.AppContext;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Options;
import qingzhou.api.Request;
import qingzhou.api.type.Editable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Model(name = "startupargs", icon = "file-code", entryAction = Editable.ACTION_NAME_EDIT, nameI18n = {"启动参数", "en:Startup Args"}, infoI18n = {"管理TongWeb的启动参数。", "en:Manage TongWeb start-up arguments."})
public class StartupArgs extends ModelBase implements Editable {
    private static final String[] mustStartsWithFlags = {"-X", "-D", "-agentlib", "-server", "-client", "-javaagent", "-verbose"}; // 如果直接增加参数 aaa 没有前缀，会重启启动不了
    private static final String SUPPORTED_JRE_KEY = "supportedJRE";
    private static final String IF_GREATER_OR_EQUAL_KEY = "range";
    private static final String PLUS = "+";
    private static final String MINUS = "-";
    private static final String EQUAL = "=";
    private static final String STARTUP_ARGS_SKIP_CHARACTER_CHECK = "%${};";

    private static final String[] invalidChars = {"#", "?", "&", " ", "`", "|", "(", ")", "@", "!", "^", "*"};// for #ITAIT-4940 replenish for NC-3285

    @Override
    public void init() {
        AppContext appContext = getAppContext();
        appContext.addI18n("jre.limit.none", new String[]{"不限", "en:No Limited"});
        appContext.addI18n("validator.arg.dataInvalid", new String[]{"不允许使用空白字符或特殊字符：" + Arrays.toString(StartupArgs.invalidChars), "en:Special characters " + Arrays.toString(StartupArgs.invalidChars) + " are not allowed"});
        appContext.addI18n("validator.sysmanaged", new String[]{"请勿修改 TongWeb 系统参数，这可能影响系统的正常运行", "en:Do not modify TongWeb system parameters, this may affect the normal operation of the system"});
        appContext.addI18n("validator.mustStartsWith", new String[]{"必需以 %s 开头", "en:Must start with %s"});
    }

    /**
     * 参数
     */
    @ModelField(required = true,
            checkXssLevel1 = true,
            skipCharacterCheck = STARTUP_ARGS_SKIP_CHARACTER_CHECK,
            showToList = true, showToEdit = false, maxLength = 1024, nameI18n = {"参数", "en:Argument"}, infoI18n = {"该参数将用于 JVM 启动时的进程入参。", "en:This argument will be used for the process entry when the JVM is started."})
    public String id;

    @ModelField(
            skipCharacterCheck = STARTUP_ARGS_SKIP_CHARACTER_CHECK,
            checkXssLevel1 = true,
            required = true, nameI18n = {"更改为", "en:Change to"}, infoI18n = {"将参数更改为此值。", "en:Change the argument to this value."})
    public String changeToArg;

    @ModelField(type = FieldType.bool, showToList = true, nameI18n = {"启用", "en:Enabled"}, infoI18n = {"只有启用的参数才会传给 JVM 加载，未启用的则不会。", "en:Only arguments that are enabled are passed to the JVM for loading, those that are not are not."})
    public Boolean enabled = true;

    @ModelField(type = FieldType.bool, showToList = true, nameI18n = {"仅 Linux 有效", "en:Only For Linux"}, infoI18n = {"开启后，该参数仅会在 linux 操作系统上启用。", "en:When turned on, this parameter is only enabled on linux operating systems."})
    public Boolean onlyForLinux = false;

    /**
     * 支持JRE版本
     */
    @ModelField(type = FieldType.select, showToList = true, nameI18n = {"限定 JRE 版本", "en:Limited JRE"}, infoI18n = {"限定该参数支持的 JRE 的版本，限定后，只有限定的 JRE 可以加载到该参数；其它 JRE 则不会，为空表示不限制。", "en:Limit the version of JRE supported by this parameter. After the limitation, only the limited JRE can be loaded into this parameter, and other JREs will not. If it is empty, it means no limitation."})
    public String supportedJRE;

    /**
     * 兼容方向
     */
    @ModelField(type = FieldType.radio, showToList = true, effectiveWhen = SUPPORTED_JRE_KEY + "!=", nameI18n = {"限定区间", "en:Limited Range"}, infoI18n = {"限定支持的 JRE 版本区间，大于、等于或者小于指定的 JRE 版本号。 + 表示大于等于，= 表示等于，- 表示小于等于。", "en:Limit the supported JRE version interval to greater than, equal to or less than the specified JRE version number."})
    public String range;

    /**
     * 参数
     */
    @ModelField(nameI18n = {"描述", "en:Description"}, infoI18n = {"该参数的描述信息。", "en:The descriptive information for this argument."})
    public String desc;

    @Override
    public Options options(Request request, String fieldName) {
        if (SUPPORTED_JRE_KEY.equals(fieldName)) {
            List<String> list = new ArrayList<String>() {{
                add("");
                for (int i = 8; i <= 21; i++) {
                    add(String.valueOf(i));
                }
            }};
            return Options.of(list.toArray(new String[0]));
        }

        if (IF_GREATER_OR_EQUAL_KEY.equals(fieldName)) {
            return Options.of(PLUS, EQUAL, MINUS);
        }

        return super.options(request, fieldName);
    }

    /**
     * 〈将前端数据，转换成后端存储数据格式〉
     *
     * @author LiJingJing 2021/11/5 10:06
     */
//  todo  @Override
//    protected Properties rectifyParameters(Properties properties) throws Exception {
//        String jre = (String) properties.remove(SUPPORTED_JRE_KEY);
//        String flag = (String) properties.remove(IF_GREATER_OR_EQUAL_KEY);
//
//        if (jre == null) { // for #ITAIT-4568
//            return properties;
//        } else {
//            properties.put(SUPPORTED_JRE_KEY, jre);
//        }
//
//
//        if (!Utils.isBlank(flag) && (PLUS.equals(flag) || MINUS.equals(flag))) {
//            properties.put(SUPPORTED_JRE_KEY, jre + flag);
//        }
//
//        return super.rectifyParameters(properties);
//    }

//  todo  @Override
//    public void show(ActionContext actionContext) throws Exception {
//        super.show(actionContext);
//        rectifyModels(actionContext.getModels());
//    }

//  todo  @Override
//    public void listInternal(ActionContext actionContext) throws Exception {
//        super.listInternal(actionContext);
//        List<ModelBase> args = actionContext.getModels();
//        rectifyModels(args);
//    }

//  todo  private void rectifyModels(List<ModelBase> args) {
//        for (ModelBase model : args) {
//            StartupArgs arg = (StartupArgs) model;
//            arg.changeToArg = arg.name;// 校验 changeToArg 时候的 List<String> otherValues 需要这个
//            String ver = arg.supportedJRE;
//            if (Utils.notBlank(ver)) {
//                String lastFlag = ver.substring(ver.length() - 1);
//                if (PLUS.equals(lastFlag)) {
//                    arg.range = PLUS;
//                    arg.supportedJRE = ver.substring(0, ver.length() - 1);
//                } else if (MINUS.equals(lastFlag)) {
//                    arg.range = MINUS;
//                    arg.supportedJRE = ver.substring(0, ver.length() - 1);
//                } else {
//                    arg.range = EQUAL;
//                }
//            }
//        }
//    }

//    @Override
//    public void delete(ActionContext actionContext) throws Exception {
//        for (String managedPrefix : Constants.StartupArgs.systemManagedPrefix) {
//            if (actionContext.getId().startsWith(managedPrefix)) {
//                actionContext.setSuccess(false);
//                actionContext.setMsg(I18n.getString("validator.sysmanaged"));
//                return;
//            }
//        }
//
//        super.delete(actionContext);
//    }
//
//    @Override
//    public String validate(Request request, String fieldName) {
//        if (Createable.ACTION_NAME_ADD.equals(request.getActionName())) { // 创建新参数
//            if ("name".equals(fieldName)) {
//                String validateArg = validateArg(newValue, otherValues, oldValue);
//                if (validateArg != null) {
//                    return validateArg;
//                }
//            }
//        } else { // 编辑已有参数 （可能的bug：name整体全部更新变成一个新参数，例如 -Xms 更新为 -Xmx，本质是删除+创建，验证可能不正确）
//            if ("changeToArg".equals(fieldName)) {
//                String validateArg = validateArg(newValue, otherValues, oldValue);
//                if (validateArg != null) {
//                    return validateArg;
//                }
//            }
//        }
//
//        return super.validate(request, fieldName);
//    }
//
//    private String validateArg(String newValue, List<String> otherValues, String oldValue) {
//        for (String managedPrefix : Constants.StartupArgs.systemManagedPrefix) {
//            if ((Utils.notBlank(newValue) && newValue.startsWith(managedPrefix)) || (Utils.notBlank(oldValue) && oldValue.startsWith(managedPrefix))) {
//                return I18n.getString("validator.sysmanaged");
//            }
//        }
//
//        if (Utils.isBlank(newValue)
//                || "-D".equals(newValue)
//                || "-X".equals(newValue)) {
//            return I18n.getString("validator.require");
//        }
//
//        if (otherValues.contains(newValue)) {
//            return I18n.getString("validator.exist");
//        }
//
//        //排除一些不能在url中传递的参数
//        for (String s : StartupArgs.invalidChars) {
//            if (newValue.contains(s)) {
//                return I18n.getString("validator.arg.dataInvalid");
//            }
//        }
//
//        // 1. 验证内存类参数的取值格式
//        List<String> checkValueFormat = new ArrayList<>();
//        checkValueFormat.addAll(Arrays.asList(Constants.StartupArgs.uniqueMemoryArgKeys));
//        checkValueFormat.addAll(Arrays.asList(Constants.StartupArgs.uniqueMemoryArgKeyPairs));
//        for (String k : checkValueFormat) {
//            if (newValue.startsWith(k)) {
//                try {
//                    getValueAsByte(newValue, k);
//                } catch (IllegalArgumentException e) {
//                    return I18n.getString("validator.arg.memory.invalid");
//                }
//                break;
//            }
//        }
//        for (String k : Constants.StartupArgs.uniqueBooleanArgKeys) {
//            if (newValue.startsWith(k)) {
//                String b = newValue.substring(k.length()).toLowerCase();
//                boolean isBool = "true".equals(b) || "false".equals(b);
//                if (!isBool) {
//                    return String.format(I18n.getString("validator.optionRange"), "[true, false]");
//                }
//                break;
//            }
//        }
//
//        // 2. 检查 最小-最大 取值比较
//        for (int i = 0; i < Constants.StartupArgs.uniqueMemoryArgKeyPairs.length; i += 2) {
//            String minKey = Constants.StartupArgs.uniqueMemoryArgKeyPairs[i];
//            String maxKey = Constants.StartupArgs.uniqueMemoryArgKeyPairs[i + 1];
//
//            if (newValue.startsWith(minKey)) {
//                for (String test : otherValues) {
//                    if (test.startsWith(maxKey)) {
//                        if (getValueAsByte(newValue, minKey) > getValueAsByte(test, maxKey)) {
//                            String msg = I18n.getString("validator.larger.cannot");
//                            return String.format(msg, getKeyMsg(maxKey));
//                        }
//                        break;
//                    }
//                }
//                break;
//            }
//            if (newValue.startsWith(maxKey)) {
//                for (String test : otherValues) {
//                    if (test.startsWith(minKey)) {
//                        if (getValueAsByte(newValue, maxKey) < getValueAsByte(test, minKey)) {
//                            String msg = I18n.getString("validator.less.cannot");
//                            return String.format(msg, getKeyMsg(minKey));
//                        }
//                        break;
//                    }
//                }
//                break;
//            }
//        }
//
//        // 重复性检查
//        for (String k : Constants.StartupArgs.uniqueArgs) {
//            if (newValue.startsWith(k)) {
//                if (Utils.isBlank(oldValue) || !oldValue.startsWith(k)) { // 兼容编辑
//                    for (String otherValue : otherValues) {
//                        if (otherValue.startsWith(k)) {
//                            return I18n.getString("validator.exist");
//                        }
//                    }
//                }
//                break;
//            }
//        }
//
//        boolean check = false;
//        for (String startsWith : mustStartsWithFlags) {
//            if (newValue.startsWith(startsWith)) {
//                check = true;
//                break;
//            }
//        }
//        if (!check) {
//            return String.format(I18n.getString("validator.mustStartsWith"), Arrays.toString(mustStartsWithFlags));
//        }
//
//        return null;
//    }
//
//    private String getKeyMsg(String key) {
//        return key.endsWith("=") ? key.substring(0, key.length() - 1) : key;
//    }
//
//    private long getValueAsByte(String val, String prefix) throws IllegalArgumentException {
//        String v = val;
//        v = v.substring(prefix.length());
//        if (Utils.isBlank(v)) {
//            throw new IllegalArgumentException(val);
//        }
//        String unit = v.substring(v.length() - 1);
//        if (Utils.notBlank(unit)) {
//            int level = -1;
//            if (unit.equalsIgnoreCase("M")) {
//                level = 1024;
//            } else if (unit.equalsIgnoreCase("G")) {
//                level = 1024 * 1024;
//            } else {
//                if (Character.isDigit(unit.charAt(0))) {
//                    level = 1;
//                }
//            }
//            if (level < 1) {
//                throw new IllegalArgumentException(val + ":" + level);
//            }
//
//            if (level == 1) {
//                return Long.parseLong(v);
//            } else {
//                return Long.parseLong(v.substring(0, v.length() - 1)) * level;
//            }
//        } else {
//            throw new IllegalArgumentException(val);
//        }
//    }
}
