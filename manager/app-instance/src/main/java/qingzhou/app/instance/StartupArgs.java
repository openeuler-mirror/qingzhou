package qingzhou.app.instance;

import qingzhou.api.*;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Listable;

import java.util.*;

@Model(code = "startupargs", icon = "file-code",
        name = {"启动参数", "en:Startup Args"}, info = {"管理TongWeb的启动参数。", "en:Manage TongWeb start-up arguments."})
public class StartupArgs extends ModelBase implements Createable {
    private static final String[] mustStartsWithFlags = {"-X", "-D", "-agentlib", "-server", "-client", "-javaagent", "-verbose"}; // 如果直接增加参数 aaa 没有前缀，会重启启动不了
    private static final String IF_GREATER_OR_EQUAL_KEY = "range";
    private static final String SUPPORTED_JRE_KEY = "supportedJRE";

    private static final String PLUS = "+";
    private static final String MINUS = "-";
    private static final String EQUAL = "=";
    private static final String STARTUP_ARGS_SKIP_CHARACTER_CHECK = "%${};";

    private static final String[] invalidChars = {"#", "?", "&", " ", "`", "|", "(", ")", "@", "!", "^", "*"};// for #ITAIT-4940 replenish for NC-3285

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
            list = true,
            name = {"参数", "en:Argument"},
            info = {"该参数将用于 JVM 启动时的进程入参。", "en:This argument will be used for the process entry when the JVM is started."})
    public String id;

    @ModelField(createable = false,
            name = {"更改为", "en:Change to"},
            info = {"将参数更改为此值。", "en:Change the argument to this value."})
    public String changeToArg;

    @ModelField(type = FieldType.bool, list = true, required = false,
            name = {"启用", "en:Enabled"},
            info = {"只有启用的参数才会传给 JVM 加载，未启用的则不会。", "en:Only arguments that are enabled are passed to the JVM for loading, those that are not are not."})
    public Boolean enabled = true;

    @ModelField(type = FieldType.bool, list = true, required = false,
            name = {"仅 Linux 有效", "en:Only For Linux"},
            info = {"开启后，该参数仅会在 linux 操作系统上启用。", "en:When turned on, this parameter is only enabled on linux operating systems."})
    public Boolean onlyForLinux = false;

    /**
     * 支持JRE版本
     */
    @ModelField(type = FieldType.select, required = false,
            options = {"", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21"},
            list = true, name = {"限定 JRE 版本", "en:Limited JRE"},
            info = {"限定该参数支持的 JRE 的版本，限定后，只有限定的 JRE 可以加载到该参数；其它 JRE 则不会，为空表示不限制。", "en:Limit the version of JRE supported by this parameter. After the limitation, only the limited JRE can be loaded into this parameter, and other JREs will not. If it is empty, it means no limitation."})
    private String supportedJRE;

    /**
     * 兼容方向
     */
    @ModelField(type = FieldType.radio, list = true, required = false,
            options = {PLUS, EQUAL, MINUS}, show = SUPPORTED_JRE_KEY + "!=",
            name = {"限定区间", "en:Limited Range"},
            info = {"限定支持的 JRE 版本区间，大于、等于或者小于指定的 JRE 版本号。 + 表示大于等于，= 表示等于，- 表示小于等于。", "en:Limit the supported JRE version interval to greater than, equal to or less than the specified JRE version number."})
    public String range;

    /**
     * 参数
     */
    @ModelField(required = false,
            name = {"描述", "en:Description"},
            info = {"该参数的描述信息。", "en:The descriptive information for this argument."})
    public String desc;

    @ModelAction(
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request, Response response) throws Exception {
        String id = request.getParameter(Listable.FIELD_NAME_ID);
        if (getDataStore().exists(id)) {
            response.setSuccess(false);
            response.setMsg(appContext.getI18n(request.getLang(), "validator.exist"));
            return;
        }

        Map<String, String> newData = request.getParameters();
        Map<String, String> oldData = getDataStore().getDataById(request.getId());
        String error = validateArg(request, id, new ArrayList<>(), oldData == null ? null : oldData.get(Listable.FIELD_NAME_ID));
        if (error != null) {
            response.setSuccess(false);
            response.setMsg(error);
            return;
        }
        getDataStore().addData(id, newData);
    }

    @ModelAction(
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
        Map<String, String> newData = request.getParameters();
        Map<String, String> oldData = getDataStore().getDataById(request.getId());

        String error = validateArg(request, newData.get("changeToArg"), new ArrayList<>(), oldData == null ? null : oldData.get(Listable.FIELD_NAME_ID));
        if (error != null) {
            response.setSuccess(false);
            response.setMsg(error);
            return;
        }

        getDataStore().updateDataById(request.getId(), newData);
    }

    @ModelAction(batch = true,
            name = {"删除", "en:Delete"},
            info = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request, Response response) throws Exception {
        for (String managedPrefix : StartupArg.systemManagedPrefix) {
            if (request.getId().startsWith(managedPrefix)) {
                response.setSuccess(false);
                response.setMsg(appContext.getI18n(request.getLang(), "validator.sysmanaged"));
                return;
            }
        }

        getDataStore().deleteDataById(request.getId());
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
                if (oldValue.isEmpty() || !oldValue.startsWith(k)) { // 兼容编辑
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
            throw new IllegalArgumentException(val);
        }
        String unit = v.substring(v.length() - 1);
        if (!unit.isEmpty()) {
            int level = -1;
            if (unit.equalsIgnoreCase("M")) {
                level = 1024;
            } else if (unit.equalsIgnoreCase("G")) {
                level = 1024 * 1024;
            } else {
                if (Character.isDigit(unit.charAt(0))) {
                    level = 1;
                }
            }
            if (level < 1) {
                throw new IllegalArgumentException(val + ":" + level);
            }

            if (level == 1) {
                return Long.parseLong(v);
            } else {
                return Long.parseLong(v.substring(0, v.length() - 1)) * level;
            }
        } else {
            throw new IllegalArgumentException(val);
        }
    }

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
        // Qingzhou 专用 -D 参数，不允许复写
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
    public DataStore getDataStore() {
        return null;
    }

//    private final StartUpArgsDataStore startUpArgsDataStore = new StartUpArgsDataStore();

//    private static class StartUpArgsDataStore implements DataStore {
//        @Override
//        public List<Map<String, String>> getAllData() throws Exception {
//            Config config = InstanceApp.getService(Config.class);
//            Json json = InstanceApp.getService(Json.class);
//            try (Reader reader = new FileReader(config.getConfigFile())) {
//
//                List list = json.fromJson(reader, List.class, "jvm", "arg");
//            }
//
//            JsonObject jsonObject = readJsonFile();
//            if (jsonObject != null) {
//                List<Map<String, String>> list = new ArrayList<>();
//                JsonArray args = getArgArray(jsonObject);
//                for (JsonElement arg : args) {
//                    Map map = gson.fromJson(arg, Map.class);
//                    map.put(Listable.FIELD_NAME_ID, map.get("name"));
//                    list.add(map);
//                }
//
//                rectifyModels(list);
//
//                list.sort(Comparator.comparing(o -> o.get("name")));
//
//                return list;
//            }
//
//            return null;
//        }
//
//        private void rectifyModels(List<Map<String, String>> args) {
//            for (Map<String, String> arg : args) {
//                arg.put("changeToArg", arg.get(Listable.FIELD_NAME_ID));// 校验 changeToArg 时候的 List<String> otherValues 需要这个
//                String ver = arg.get(SUPPORTED_JRE_KEY);
//                if (ver != null && !ver.isEmpty()) {
//                    String lastFlag = ver.substring(ver.length() - 1);
//                    if (PLUS.equals(lastFlag)) {
//                        arg.put(IF_GREATER_OR_EQUAL_KEY, PLUS);
//                        arg.put(SUPPORTED_JRE_KEY, ver.substring(0, ver.length() - 1));
//                    } else if (MINUS.equals(lastFlag)) {
//                        arg.put(IF_GREATER_OR_EQUAL_KEY, MINUS);
//                        arg.put(SUPPORTED_JRE_KEY, ver.substring(0, ver.length() - 1));
//                    } else {
//                        arg.put(IF_GREATER_OR_EQUAL_KEY, EQUAL);
//                    }
//                }
//                arg.put("enabled", (arg.get("enabled") == null) ? "true" : arg.get("enabled"));
//                arg.put("onlyForLinux", (arg.get("onlyForLinux") == null) ? "false" : arg.get("onlyForLinux"));
//                arg.put("changeToArg", arg.get("name"));
//            }
//        }
//
//        @Override
//        public void addData(String id, Map<String, String> data) throws Exception {
//            JsonObject jsonObject = readJsonFile();
//
//            if (jsonObject != null) {
//                JsonArray args = getArgArray(jsonObject);
//                JsonObject arg = new JsonObject();
//                for (String name : data.keySet()) {
//                    if (Listable.FIELD_NAME_ID.equals(name)) {
//                        arg.addProperty("name", data.get(name));
//                    } else {
//                        arg.addProperty(name, data.get(name));
//                    }
//                }
//                args.add(arg);
//
//                writeJsonFile(jsonObject);
//            }
//        }
//
//        @Override
//        public void updateDataById(String id, Map<String, String> data) throws Exception {
//            JsonObject jsonObject = readJsonFile();
//
//            if (jsonObject != null) {
//                JsonArray args = getArgArray(jsonObject);
//                for (JsonElement element : args) {
//                    JsonObject arg = element.getAsJsonObject();
//                    if (arg.get("name").getAsString().equals(id)) {
//                        for (String name : data.keySet()) {
//                            if (Listable.FIELD_NAME_ID.equals(name) || "changeToArg".equals(name)) {
//                                continue;
//                            }
//                            arg.addProperty(name, data.get(name));
//                        }
//                        arg.addProperty("name", data.get("changeToArg"));
//                        break;
//                    }
//                }
//
//                writeJsonFile(jsonObject);
//            }
//        }
//
//        @Override
//        public void deleteDataById(String id) throws Exception {
//            JsonObject jsonObject = readJsonFile();
//
//            if (jsonObject != null) {
//                JsonArray args = getArgArray(jsonObject);
//                for (int i = 0; i < args.size(); i++) {
//                    JsonObject arg = args.get(i).getAsJsonObject();
//                    if (arg.get("name").getAsString().equals(id)) {
//                        args.remove(i);
//                        break;
//                    }
//                }
//
//                writeJsonFile(jsonObject);
//            }
//        }
//    }
}
