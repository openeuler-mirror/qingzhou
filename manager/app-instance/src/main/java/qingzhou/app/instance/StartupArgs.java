package qingzhou.app.instance;

import qingzhou.api.*;
import qingzhou.api.type.Editable;

import java.util.Arrays;

@Model(code = "startupargs", icon = "file-code", entrance = Editable.ACTION_NAME_EDIT, name = {"启动参数", "en:Startup Args"}, info = {"管理TongWeb的启动参数。", "en:Manage TongWeb start-up arguments."})
public class StartupArgs extends ModelBase implements Editable {
    private static final String[] mustStartsWithFlags = {"-X", "-D", "-agentlib", "-server", "-client", "-javaagent", "-verbose"}; // 如果直接增加参数 aaa 没有前缀，会重启启动不了
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
    @ModelField(
            list = true,
            name = {"参数", "en:Argument"},
            info = {"该参数将用于 JVM 启动时的进程入参。", "en:This argument will be used for the process entry when the JVM is started."})
    public String id;

    @ModelField(
            name = {"更改为", "en:Change to"},
            info = {"将参数更改为此值。", "en:Change the argument to this value."})
    public String changeToArg;

    @ModelField(list = true, name = {"启用", "en:Enabled"}, info = {"只有启用的参数才会传给 JVM 加载，未启用的则不会。", "en:Only arguments that are enabled are passed to the JVM for loading, those that are not are not."})
    public Boolean enabled = true;

    @ModelField(list = true, name = {"仅 Linux 有效", "en:Only For Linux"}, info = {"开启后，该参数仅会在 linux 操作系统上启用。", "en:When turned on, this parameter is only enabled on linux operating systems."})
    public Boolean onlyForLinux = false;

    /**
     * 兼容方向
     */
    @ModelField(list = true, name = {"限定区间", "en:Limited Range"}, info = {"限定支持的 JRE 版本区间，大于、等于或者小于指定的 JRE 版本号。 + 表示大于等于，= 表示等于，- 表示小于等于。", "en:Limit the supported JRE version interval to greater than, equal to or less than the specified JRE version number."})
    public String range;

    /**
     * 参数
     */
    @ModelField(name = {"描述", "en:Description"}, info = {"该参数的描述信息。", "en:The descriptive information for this argument."})
    public String desc;

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
