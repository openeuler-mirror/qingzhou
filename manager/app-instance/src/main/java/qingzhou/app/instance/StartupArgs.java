package qingzhou.app.instance;

import qingzhou.api.*;
import qingzhou.api.type.Addable;
import qingzhou.config.Arg;
import qingzhou.config.Config;
import qingzhou.config.Jvm;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.Utils;

import java.util.*;

@Model(code = "startupargs", icon = "file-code",
        name = {"启动参数", "en:Startup Args"}, info = {"管理TongWeb的启动参数。", "en:Manage TongWeb start-up arguments."})
public class StartupArgs extends ModelBase implements Addable {
    private static final String[] mustStartsWithFlags = {"-X", "-D", "-agentlib", "-server", "-client", "-javaagent", "-verbose"}; // 如果直接增加参数 aaa 没有前缀，会重启启动不了
    private static final String IF_GREATER_OR_EQUAL_KEY = "range";
    private static final String SUPPORTED_JRE_KEY = "supportedJRE";

    private static final String PLUS = "+";
    private static final String MINUS = "-";
    private static final String EQUAL = "=";
    private static final String STARTUP_ARGS_SKIP_CHARACTER_CHECK = "%${};";

    private static final String[] invalidChars = {"#", "?", "&", " ", "`", "|", "(", ")", "@", "!", "^", "*"};// for #ITAIT-4940 replenish for NC-3285

    static class StartupArg {
        public static final String[] uniqueMemoryArgKeys = {"-XX:MaxDirectMemorySize="};
        public static final String[] uniqueMemoryArgKeyPairs = { // 最小 - 最大，须成对
                "-Xms", "-Xmx", // 最小 - 最大，须成对
                "-XX:NewSize=", "-XX:MaxNewSize=",  // 最小 - 最大，须成对
                "-XX:MetaspaceSize=", "-XX:MaxMetaspaceSize=", // 最小 - 最大，须成对
                "-XX:InitialHeapSize=", "-XX:MaxHeapSize=" // 最小 - 最大，须成对
        };
        public static final String[] uniqueBooleanArgKeys = {"-Djava.awt.headless="};
        public static final String[] uniqueArgKeys = {"-Xloggc:", "-XX:LogFile=", "-Djava.security.policy=", "-Duser.language=", "-Djava.security.egd=", "-agentlib:jdwp=",};
        public static final String[] uniqueArgVals = {"-XX:+HeapDumpOnOutOfMemoryError", "-XX:+DisableExplicitGC", "-XX:+PrintGCDetails", "-XX:+UnlockDiagnosticVMOptions", "-XX:+LogVMOutput", "-Djava.security.manager", "-server"};
        // QingZhou 专用 -D 参数，不允许复写
        public static final String[] systemManagedPrefix = {"-Dqingzhou.home", "-Dqingzhou.base", "-Djava.util.logging.manager"};
        public static final Set<String> uniqueArgs = new HashSet<>();

        static {
            uniqueArgs.addAll(Arrays.asList(StartupArg.uniqueMemoryArgKeys));
            uniqueArgs.addAll(Arrays.asList(StartupArg.uniqueMemoryArgKeyPairs));
            uniqueArgs.addAll(Arrays.asList(StartupArg.uniqueBooleanArgKeys));
            uniqueArgs.addAll(Arrays.asList(StartupArg.uniqueArgKeys));
            uniqueArgs.addAll(Arrays.asList(StartupArg.uniqueArgVals));
            uniqueArgs.addAll(Arrays.asList(StartupArg.systemManagedPrefix));
        }
    }

    @Override
    public void start() {
        appContext.addI18n("jre.limit.none", new String[]{"不限", "en:No Limited"});
        appContext.addI18n("validator.arg.dataInvalid", new String[]{"不允许使用空白字符或特殊字符：" + Arrays.toString(StartupArgs.invalidChars), "en:Special characters " + Arrays.toString(StartupArgs.invalidChars) + " are not allowed"});
        appContext.addI18n("validator.sysmanaged", new String[]{"请勿修改 TongWeb 系统参数，这可能影响系统的正常运行", "en:Do not modify TongWeb system parameters, this may affect the normal operation of the system"});
        appContext.addI18n("validator.mustStartsWith", new String[]{"必需以 %s 开头", "en:Must start with %s"});
    }

    /**
     * 参数
     */
    @ModelField(
            required = true,
            list = true,
            name = {"参数", "en:Argument"},
            info = {"该参数将用于 JVM 启动时的进程入参。", "en:This argument will be used for the process entry when the JVM is started."})
    public String id;

    @ModelField(
            required = true,
            createable = false,
            name = {"更改为", "en:Change to"},
            info = {"将参数更改为此值。", "en:Change the argument to this value."})
    public String changeToArg;

    @ModelField(type = FieldType.bool, list = true,
            name = {"启用", "en:Enabled"},
            info = {"只有启用的参数才会传给 JVM 加载，未启用的则不会。", "en:Only arguments that are enabled are passed to the JVM for loading, those that are not are not."})
    public Boolean enabled = true;

    @ModelField(type = FieldType.bool, list = true,
            name = {"仅 Linux 有效", "en:Only For Linux"},
            info = {"开启后，该参数仅会在 linux 操作系统上启用。", "en:When turned on, this parameter is only enabled on linux operating systems."})
    public Boolean forLinux = false;

    /**
     * 支持JRE版本
     */
    @ModelField(type = FieldType.select,
            options = {"", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21"},
            list = true, name = {"限定 JRE 版本", "en:Limited JRE"},
            info = {"限定该参数支持的 JRE 的版本，限定后，只有限定的 JRE 可以加载到该参数；其它 JRE 则不会，为空表示不限制。", "en:Limit the version of JRE supported by this parameter. After the limitation, only the limited JRE can be loaded into this parameter, and other JREs will not. If it is empty, it means no limitation."})
    public String supportedJRE;

    /**
     * 兼容方向
     */
    @ModelField(type = FieldType.radio, list = true,
            options = {PLUS, EQUAL, MINUS}, show = SUPPORTED_JRE_KEY + "!=",
            name = {"限定区间", "en:Limited Range"},
            info = {"限定支持的 JRE 版本区间，大于、等于或者小于指定的 JRE 版本号。 + 表示大于等于，= 表示等于，- 表示小于等于。", "en:Limit the supported JRE version interval to greater than, equal to or less than the specified JRE version number."})
    public String range;

    /**
     * 参数
     */
    @ModelField(
            name = {"描述", "en:Description"},
            info = {"该参数的描述信息。", "en:The descriptive information for this argument."})
    public String desc;

    @ModelAction(
            code = DeployerConstants.ACTION_ADD,
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request) throws Exception {
        Response response = request.getResponse();
        Map<String, String> oldData = showData(request.getId());
        if (oldData != null) {
            response.setSuccess(false);
            response.setMsg(appContext.getI18n(request.getLang(), "validator.exist"));
            return;
        }

        Map<String, String> newData = request.getParameters();
        String error = validateArg(request, request.getId(), new ArrayList<>(), null);
        if (error != null) {
            response.setSuccess(false);
            response.setMsg(error);
            return;
        }

        processData(newData);

        addData(newData);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_UPDATE,
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request) throws Exception {
        Map<String, String> newData = request.getParameters();
        Map<String, String> oldData = showData(request.getId());

        String error = validateArg(request, newData.get("changeToArg"), new ArrayList<>(), oldData == null ? null : oldData.get(idFieldName()));
        if (error != null) {
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(error);
            return;
        }

        processData(newData);

        updateData(newData);
    }

    private void processData(Map<String, String> newData) {
        newData.put("name", newData.getOrDefault("changeToArg", newData.get(idFieldName())));
        String supportedJre = newData.getOrDefault(SUPPORTED_JRE_KEY, "");
        if (!supportedJre.trim().isEmpty()) {
            supportedJre = supportedJre + newData.getOrDefault(IF_GREATER_OR_EQUAL_KEY, "");
        }
        newData.put(SUPPORTED_JRE_KEY, supportedJre);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_DELETE,
            batch = true,
            name = {"删除", "en:Delete"},
            info = {"删除本条数据，注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this data, note: Please operate with caution, it cannot be restored after deletion."})
    public void delete(Request request) throws Exception {
        for (String managedPrefix : StartupArg.systemManagedPrefix) {
            if (request.getId().startsWith(managedPrefix)) {
                request.getResponse().setSuccess(false);
                request.getResponse().setMsg(appContext.getI18n(request.getLang(), "validator.sysmanaged"));
                return;
            }
        }

        deleteData(request.getId());
    }

    private String validateArg(Request request, String newValue, List<String> otherValues, String oldValue) {
        for (String managedPrefix : StartupArg.systemManagedPrefix) {
            if ((newValue != null && !newValue.isEmpty() && newValue.startsWith(managedPrefix)) || (oldValue != null && !oldValue.isEmpty() && oldValue.startsWith(managedPrefix))) {
                return appContext.getI18n(request.getLang(), "validator.sysmanaged");
            }
        }

        if ((newValue == null || newValue.isEmpty())
                || "-D".equals(newValue)
                || "-X".equals(newValue)) {
            return appContext.getI18n(request.getLang(), "validator.require");
        }

        if (otherValues.contains(newValue)) {
            return appContext.getI18n(request.getLang(), "validator.exist");
        }

        //排除一些不能在url中传递的参数
        for (String s : StartupArgs.invalidChars) {
            if (newValue.contains(s)) {
                return appContext.getI18n(request.getLang(), "validator.arg.dataInvalid");
            }
        }

        // 1. 验证内存类参数的取值格式
        List<String> checkValueFormat = new ArrayList<>();
        checkValueFormat.addAll(Arrays.asList(StartupArg.uniqueMemoryArgKeys));
        checkValueFormat.addAll(Arrays.asList(StartupArg.uniqueMemoryArgKeyPairs));
        for (String k : checkValueFormat) {
            if (newValue.startsWith(k)) {
                try {
                    getValueAsByte(newValue, k);
                } catch (IllegalArgumentException e) {
                    return appContext.getI18n(request.getLang(), "validator.arg.memory.invalid");
                }
                break;
            }
        }
        for (String k : StartupArg.uniqueBooleanArgKeys) {
            if (newValue.startsWith(k)) {
                String b = newValue.substring(k.length()).toLowerCase();
                boolean isBool = "true".equals(b) || "false".equals(b);
                if (!isBool) {
                    return String.format(appContext.getI18n(request.getLang(), "validator.optionRange"), "[true, false]");
                }
                break;
            }
        }

        // 2. 检查 最小-最大 取值比较
        for (int i = 0; i < StartupArg.uniqueMemoryArgKeyPairs.length; i += 2) {
            String minKey = StartupArg.uniqueMemoryArgKeyPairs[i];
            String maxKey = StartupArg.uniqueMemoryArgKeyPairs[i + 1];

            if (newValue.startsWith(minKey)) {
                for (String test : otherValues) {
                    if (test.startsWith(maxKey)) {
                        if (getValueAsByte(newValue, minKey) > getValueAsByte(test, maxKey)) {
                            String msg = appContext.getI18n(request.getLang(), "validator.larger.cannot");
                            return String.format(msg, getKeyMsg(maxKey));
                        }
                        break;
                    }
                }
                break;
            }
            if (newValue.startsWith(maxKey)) {
                for (String test : otherValues) {
                    if (test.startsWith(minKey)) {
                        if (getValueAsByte(newValue, maxKey) < getValueAsByte(test, minKey)) {
                            String msg = appContext.getI18n(request.getLang(), "validator.less.cannot");
                            return String.format(msg, getKeyMsg(minKey));
                        }
                        break;
                    }
                }
                break;
            }
        }

        // 重复性检查
        for (String k : StartupArg.uniqueArgs) {
            if (newValue.startsWith(k)) {
                if (oldValue == null || oldValue.isEmpty() || !oldValue.startsWith(k)) { // 兼容编辑
                    for (String otherValue : otherValues) {
                        if (otherValue.startsWith(k)) {
                            return appContext.getI18n(request.getLang(), "validator.exist");
                        }
                    }
                }
                break;
            }
        }

        boolean check = false;
        for (String startsWith : mustStartsWithFlags) {
            if (newValue.startsWith(startsWith)) {
                check = true;
                break;
            }
        }
        if (!check) {
            return String.format(appContext.getI18n(request.getLang(), "validator.mustStartsWith"), Arrays.toString(mustStartsWithFlags));
        }

        return null;
    }

    private String getKeyMsg(String key) {
        return key.endsWith("=") ? key.substring(0, key.length() - 1) : key;
    }

    private long getValueAsByte(String val, String prefix) throws IllegalArgumentException {
        String v = val;
        v = v.substring(prefix.length());
        if (v.isEmpty()) {
            throw new IllegalArgumentException("Value is empty: " + val);
        }
        String unit = v.substring(v.length() - 1);
        if (!unit.isEmpty()) {
            int level = -1;
            if (Character.isDigit(unit.charAt(0))) {
                return Long.parseLong(v);
            }
            if (unit.equalsIgnoreCase("M")) {
                level = 1024;
            } else if (unit.equalsIgnoreCase("G")) {
                level = 1024 * 1024;
            }
            if (level != -1) {
                return Long.parseLong(v.substring(0, v.length() - 1)) * level;
            } else {
                throw new IllegalArgumentException("Invalid value format, " + val + ":" + level);
            }
        } else {
            throw new IllegalArgumentException("Unit is empty" + val);
        }
    }

    @Override
    public void addData(Map<String, String> data) throws Exception {
        Config config = Main.getService(Config.class);
        Arg arg = new Arg();
        Utils.setPropertiesToObj(arg, data);
        arg.setName(data.get(idFieldName()));
        config.addArg(arg);
    }

    @Override
    public void deleteData(String id) throws Exception {
        Main.getService(Config.class).deleteArg(id);
    }

    @Override
    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) throws Exception {
        Config config = Main.getService(Config.class);
        Jvm jvm = config.getJvm();
        List<Map<String, String>> list = new ArrayList<>();
        for (Arg arg : jvm.getArg()) {
            Map<String, String> map = Utils.getPropertiesFromObj(arg); //for #ITAIT-6324
            map.put(idFieldName(), map.get("name"));
            list.add(map);
        }

        list.sort(Comparator.comparing(o -> o.get("name")));

        return list;
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        Config config = Main.getService(Config.class);
        Arg arg = new Arg();
        Utils.setPropertiesToObj(arg, data);
        config.deleteArg(data.get(idFieldName()));
        config.addArg(arg);
    }

    @Override
    public Map<String, String> showData(String id) throws Exception {
        Config config = Main.getService(Config.class);
        Jvm jvm = config.getJvm();
        List<Map<String, String>> list = new ArrayList<>();
        for (Arg arg : jvm.getArg()) {
            Map<String, String> map = Utils.getPropertiesFromObj(arg); //for #ITAIT-6324
            if (map.get("name").equals(id)) return map;
        }
        return null;
    }

    // todo： 需有统一的 参数矫正机制来作用到 list和show 方法的数据上？
    private void rectifyModels(List<Map<String, String>> args) {
        for (Map<String, String> arg : args) {
            arg.put("changeToArg", arg.get(idFieldName()));// 校验 changeToArg 时候的 List<String> otherValues 需要这个
            String ver = arg.get(SUPPORTED_JRE_KEY);
            if (ver != null && !ver.isEmpty()) {
                String lastFlag = ver.substring(ver.length() - 1);
                if (PLUS.equals(lastFlag)) {
                    arg.put(IF_GREATER_OR_EQUAL_KEY, PLUS);
                    arg.put(SUPPORTED_JRE_KEY, ver.substring(0, ver.length() - 1));
                } else if (MINUS.equals(lastFlag)) {
                    arg.put(IF_GREATER_OR_EQUAL_KEY, MINUS);
                    arg.put(SUPPORTED_JRE_KEY, ver.substring(0, ver.length() - 1));
                } else {
                    arg.put(IF_GREATER_OR_EQUAL_KEY, EQUAL);
                }
            }
            arg.put("enabled", arg.getOrDefault("enabled", "true"));
            arg.put("onlyForLinux", arg.getOrDefault("onlyForLinux", "false"));
            arg.put("changeToArg", arg.get("name"));
            arg.put(IF_GREATER_OR_EQUAL_KEY, arg.getOrDefault(IF_GREATER_OR_EQUAL_KEY, ""));
            arg.put(SUPPORTED_JRE_KEY, arg.getOrDefault(SUPPORTED_JRE_KEY, ""));
        }
    }
}
