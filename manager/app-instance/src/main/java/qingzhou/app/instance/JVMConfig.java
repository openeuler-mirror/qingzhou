package qingzhou.app.instance;

import qingzhou.api.*;
import qingzhou.api.type.Editable;
import qingzhou.config.Arg;
import qingzhou.config.Config;
import qingzhou.config.Env;
import qingzhou.config.Jvm;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.I18nTool;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;
import qingzhou.json.Json;
import qingzhou.registry.AppInfo;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

import java.io.File;
import java.util.*;

@Model(code = JVMConfig.MODEL_NAME_jvmconfig, icon = "coffee", entrance = Editable.ACTION_NAME_EDIT,
        name = {"JVM 配置", "en:JVM Configuration"}, info = {"配置运行 QingZhou 应用服务器的 JVM 属性。", "en:Configure the JVM properties of the server running QingZhou applications."})
public class JVMConfig extends ModelBase implements Editable {
    public static final String MODEL_NAME_jvmconfig = "jvmconfig";
    public static final String DATA_SEPARATOR = ",";
    public static final String JAVA_HOME_KEY = "JAVA_HOME";
    private final String group_memory = "group_memory";
    private final String group_gc = "group_gc";
    private final String group_heapDump = "group_heapDump";
    private final String group_jvmLog = "group_jvmLog";
    private final String group_environment = "group_environment";
    private final String group_IP = "group_IP";

    private static final String[] sizeKeyPairs = { // 最小 - 最大，须成对
            "Xms", "Xmx", // 最小 - 最大，须成对
            "Xmn", "Xmx",  // 最小 - 最大，须成对
            "MetaspaceSize", "MaxMetaspaceSize", // 最小 - 最大，须成对
            "Xss", "Xmx" // 最小 - 最大，须成对
    };

    @Override
    public void start() {
        appContext.addI18n("validator.arg.memory.invalid", new String[]{"参数【%s】不合法，此参数应以数字加单位(m/M、g/G)构成", "en:The parameter [%s] is not legal, this parameter should be composed of numbers plus units (m/M, g/G)."});
        appContext.addI18n("validator.larger.cannot", new String[]{"%s不支持大于%s", "en:Cannot be larger than %s"});
        appContext.addI18n("validator.arg.memory.union.invalid", new String[]{"联合校验参数【%s】不合法，该参数应以数字加单位(m/M、g/G)构成", "en:The joint verification parameter [%s] is illegal, and the parameter should be composed of numbers plus units (m/m, g/g)."});

        appContext.addI18n("validator.env.notJava", new String[]{"请不要通过这里设置 JAVA_HOME 环境变量，您可使用”Java 路径“参数来设置",
                "en:Please do not set the JAVA_HOME environment variable here, you can set it using \"JAVA_HOME\" Parameter"});
        appContext.addI18n("validator.check.filePath", new String[]{"文件路径不支持以\\或者/结尾，不支持包含特殊字符和空格", "en:The file path cannot end with \\ or /, and cannot contain special characters or Spaces"});
        appContext.addI18n("validator.check.heapDumpPath", new String[]{"堆转储文件仅支持以.hprof结束", "en:Heap dumps are only supported for files ending in .hprof"});
        appContext.addI18n("validator.check.logFile", new String[]{"日志文件只支持以.log或.txt结束", "en:Log files only support ending in .log or .txt"});
        appContext.addI18n("validator.check.timeFlag", new String[]{"时间戳只能出现在文件名上，而不能在文件夹上", "en:The timestamp can only appear on the file name, not on the folder"});
    }

    @ModelField(required = false, group = group_memory, name = {"初始堆内存", "en:Initial Heap Size"}, info = {"设置初始堆内存大小 (-Xms)。", "en:Set the initial heap size (-Xms)."})
    public String Xms;

    @ModelField(required = false, group = group_memory, name = {"最大堆内存", "en:Max Heap Size"}, info = {"设置最大堆内存大小 (-Xmx)。", "en:Set the max heap size (-Xmx)."})
    public String Xmx;

    @ModelField(required = false, group = group_memory, name = {"新生代大小", "en:Xmn Size"}, info = {"设置新生代大小 (-Xmn)。", "en:Set the new generation size (-Xmn)."})
    public String Xmn;

    @ModelField(required = false, group = group_memory, show = "Xmn=", min = 1, max = 100, type = FieldType.number, name = {"老年代比率", "en:New Ratio"}, info = {"设置新生代和老年代的大小比率，通俗地讲即老年代比新生代的倍数 (-XX:NewRatio)。", "en:Set the size ratio of the new generation and the old era, which is colloquially speaking, that is, the multiple of the old era to the new generation (-XX:NewRatio)."})
    public Integer NewRatio;

    @ModelField(required = false, group = group_memory, min = 1, max = 100, type = FieldType.number, name = {"Eden 区比率", "en:Survivor Ratio"}, info = {"设置新生代中 Eden 区域和 Survivor 区域（From 幸存区或 To 幸存区）的比率 (-XX:SurvivorRatio)。", "en:Sets the ratio of the Eden zone to the Survivor zone (From Survivor Zone or To Survivor Zone) in the new generation (-XX:SurvivorRatio)."})
    public Integer SurvivorRatio;

    @ModelField(required = false, group = group_memory, name = {"初始元空间", "en:MetaspaceSize"}, info = {"设置初始元空间的大小 (-XX:MetaspaceSize)。", "en:Set the initial Metaspace size (-XX:MetaspaceSize)."})
    public String MetaspaceSize;

    @ModelField(required = false, group = group_memory, name = {"最大元空间", "en:MaxMetaspaceSize"}, info = {"设置最大元空间的大小 (-XX:MaxMetaspaceSize)。", "en:Set the max metaspace size (-XX:MaxMetaspaceSize)."})
    public String MaxMetaspaceSize;

    @ModelField(required = false, group = group_memory, name = {"线程栈大小", "en:Stack Size"}, info = {"设置线程栈大小的最大值 (-Xss)。", "en:Set the max thread stack size (-Xss)."})
    public String Xss;

    @ModelField(required = false, group = group_memory, name = {"堆外内存", "en:MaxDirectMemorySize"}, info = {"设置最大堆外内存，当 Direct ByteBuffer 分配的堆外内存达到该大小后，即触发 Full GC (-XX:MaxDirectMemorySize)。", "en:Set the maximum out-of-heap memory. When the out-of-heap memory allocated by Direct ByteBuffer reaches this size, Full GC is triggered (-XX:MaxDirectMemorySize)."})
    public String MaxDirectMemorySize;

    @ModelField(required = false, group = group_gc, type = FieldType.radio,
            options = {"", "UseSerialGC", "UseParallelGC", "UseConcMarkSweepGC", "UseG1GC"},
            name = {"垃圾回收器", "en:GarbageCollector"},
            info = {"设置 JVM 回收内存使用的垃圾回收器。", "en:Set up the garbage collector used by the JVM to reclaim memory."})
    public String useGC;

    @ModelField(required = false, group = group_gc, show = "useGC=UseParallelGC|useGC=UseConcMarkSweepGC", min = 1, max = 100, type = FieldType.number, name = {"并行收集线程数", "en:ParallelGCThreads"}, info = {"设置并行收集器收集时使用的CPU数，即并行收集线程数 (-XX:ParallelGCThreads)。", "en:Set the number of CPUs used by the parallel collector collection, that is, the number of parallel collection threads (-XX:ParallelGCThreads)."})
    public Integer ParallelGCThreads;
    @ModelField(required = false, group = group_gc, show = "useGC=UseParallelGC", min = 1, max = 100, type = FieldType.number, name = {"最大暂停时间", "en:MaxGCPauseMillis"}, info = {"每次 GC 最大的停顿毫秒数，VM 将调整 Java 堆大小和其他与 GC 相关的参数，以使 GC 引起的暂停时间短于该毫秒，尽可能地保证内存回收花费时间不超过设定值（-XX:MaxGCPauseMillis）。", "en:For each GC maximum pause milliseconds, the VM will adjust the Java heap size and other GC-related parameters so that the pause caused by GC is shorter than that millisecond, ensuring that memory reclamation takes no longer than the set value as much as possible (-XX:MaxGCPauseMillis)."})
    public Integer MaxGCPauseMillis;
    @ModelField(required = false, group = group_gc, show = "useGC=UseParallelGC", min = 1, max = 100, type = FieldType.number, name = {"回收时间占比", "en:GCTimeRatio"}, info = {"设置垃圾回收时间占程序运行时间的百分比 (-XX:GCTimeRatio)。", "en:Sets the garbage collection time as a percentage of the program running time (-XX:GCTimeRatio)."})
    public Integer GCTimeRatio;

    @ModelField(required = false, group = group_gc, type = FieldType.bool, name = {"记录 GC 日志", "en:Print GC "}, info = {"设置是否记录 JVM 的 GC 日志。", "en:Sets whether to record GC logs for the JVM."})
    public boolean gcLogEnabled;
    @ModelField(required = false, group = group_gc, show = "gcLogEnabled=true", type = FieldType.bool, name = {"记录细节", "en:GC Details"}, info = {"设置是否记录 GC 的详细信息 (-XX:+PrintGCDetails)。", "en:Set whether GC details are logged (-XX:+PrintGCDetails)."})
    public boolean PrintGCDetails;
    @ModelField(required = false, group = group_gc, show = "gcLogEnabled=true", type = FieldType.bool, name = {"记录系统停顿时间", "en:Application StoppedTime"}, info = {"是否在 GC 日志中记录系统停顿时间。仅适用于 java8 (-XX:+PrintGCApplicationStoppedTime)。", "en:Whether application StoppedTime are recorded in GC logs, only available for java8 (-XX:+PrintGCApplicationStoppedTime)."})
    public boolean PrintGCApplicationStoppedTime;
    @ModelField(required = false, group = group_gc, show = "gcLogEnabled=true", type = FieldType.bool, name = {"记录系统执行时间", "en:Application ConcurrentTime"}, info = {"是否在 GC 日志中记录系统执行时间。仅适用于 java8 (-XX:+PrintGCApplicationConcurrentTime)。", "en:Whether application ConcurrentTime are recorded in GC logs, only available for java8 (-XX:+PrintGCApplicationConcurrentTime)."})
    public boolean PrintGCApplicationConcurrentTime;
    @ModelField(required = false, group = group_gc, show = "gcLogEnabled=true", type = FieldType.bool, name = {"记录堆信息", "en:Print Heap"}, info = {"是否在 GC 日志中记录堆信息，仅适用于java8 (-XX:+PrintHeapAtGC)。", "en:Whether heap information is recorded in the GC log, only available for java8 (-XX:+PrintHeapAtGC)."})
    public boolean PrintHeapAtGC;
    @ModelField(group = group_gc, show = "gcLogEnabled=true", name = {"GC 日志文件", "en:GC Log Path"},
            info = {"设置 GC 日志的存储位置。注：路径可以是绝对路径，也可以是相对于 QingZhou 实例目录的相对路径，同时可使用 ${QZ_TimeStamp} 变量添加 QingZhou 启动时间戳 (-Xlog)。",
                    "en:Set the storage location for GC logs. Note: The path can be absolute or relative to the QingZhou instance directory, and the QingZhou startup timestamp can be added using the ${QZ_TimeStamp} variable (-Xlog)."})
    public String gcLog = "logs/gc/gc.log";

    @ModelField(required = false, group = group_heapDump, type = FieldType.bool, name = {"开启堆转储", "en:Heap Dump"}, info = {"当 JVM 发生 OOM 时，自动生成 DUMP 文件，文件位置由【堆转储文件】选项指定 (-XX:+HeapDumpOnOutOfMemoryError)。", "en:When OOM occurs in JVM, a dump file is automatically generated, and the file location is specified by the [Heap Dump Path] option (-XX:+HeapDumpOnOutOfMemoryError)."})
    public boolean HeapDumpOnOutOfMemoryError;
    @ModelField(group = group_heapDump, show = "HeapDumpOnOutOfMemoryError=true",
            name = {"堆转储文件", "en:Heap Dump Path"}, info = {"设置 JVM 发生 OOM 时，自动生成 DUMP 文件的路径。注：路径可以是绝对路径，也可以是相对于 QingZhou 域目录的相对路径，同时可使用 ${QZ_TimeStamp} 变量添加 QingZhou 启动时间戳 (-XX:HeapDumpPath)。",
            "en:Set the path to the JVM to automatically generate a dump file when OOM occurs. Note: The path can be absolute or relative to the QingZhou instance directory, and the QingZhou startup timestamp can be added using the ${QZ_TimeStamp} variable (-XX:HeapDumpPath)."})
    public String HeapDumpPath = "logs/heap_${QZ_TimeStamp}.hprof";

    @ModelField(required = false, group = group_jvmLog, type = FieldType.bool, name = {"记录 JVM 日志", "en:Log VM Output"}, info = {"设置是否记录 JVM 日志 (-XX:+LogVMOutput)。", "en:Set whether to log JVM logs (-XX:+LogVMOutput)."})
    public boolean LogVMOutput;
    @ModelField(group = group_jvmLog, show = "LogVMOutput=true",
            name = {"JVM 日志文件", "en:JVM Log Path"},
            info = {"设置 JVM 日志的存储位置。注：路径可以是绝对路径，也可以是相对于 QingZhou 域目录的相对路径，同时可使用 ${QZ_TimeStamp} 变量添加 QingZhou 启动时间戳 (-XX:LogFile)。",
                    "en:Set the location where JVM logs are stored. Note: The path can be absolute or relative to the QingZhou instance directory, and the QingZhou startup timestamp can be added using the ${QZ_TimeStamp} variable (-XX:LogFile)."})
    public String LogFile = "logs/jvm/jvm.log";

    @ModelField(required = false, group = group_environment,
            name = {"Java 路径", "en:Java Home"},
            info = {"设置运行 QingZhou 所需要的 Java 路径。", "en:Set the Java path required to run QingZhou."})
    public String JAVA_HOME;
    @ModelField(required = false, group = group_environment, type = FieldType.kv, lengthMax = 4096000, name = {"环境变量", "en:Environments"},
            info = {"设置应用程序所需要的环境变量。", "en:Set the environment variables required by the application."})
    public String envs;

    @ModelField(required = false, group = group_IP, type = FieldType.bool,
            name = {"首选 IPv4", "en:Prefer IPv4"},
            info = {"在支持 IPv4 映射地址的 IPv6 网络协议栈中，首选使用 IPv4 协议栈 (-Djava.net.preferIPv4Stack)。",
                    "en:Among IPv6 network stacks that support IPv4 mapped addresses, the IPv4 stack is preferred (-Djava.net.preferIPv4Stack)."}
    )
    public boolean preferIPv4Stack = false;

    @Override
    public Groups groups() {
        return Groups.of(
                Group.of(group_memory, new String[]{"内存大小", "en:Memory Size"}),
                Group.of(group_gc, new String[]{"GC 策略", "en:GC Strategy"}),
                Group.of(group_heapDump, new String[]{"堆转储", "en:Heap Dump"}),
                Group.of(group_jvmLog, new String[]{"JVM 日志", "en:JVM Log"}),
                Group.of(group_environment, new String[]{"环境变量", "en:Environment Variables"}),
                Group.of(group_IP, new String[]{"IP 版本", "en:IP Version"})
        );
    }


    @ModelAction(
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
        if (!validate(request, response)) {
            return;
        }

        Map<String, String> data = new HashMap<>();
        AppInfo appInfo = InstanceApp.getService(Deployer.class).getApp(DeployerConstants.INSTANCE_APP_NAME).getAppInfo();
        for (String fieldName : appInfo.getModelInfo(request.getModel()).getFormFieldNames()) {
            String value = request.getParameter(fieldName);
            if (value != null) {
                data.put(fieldName, value);
            }
        }

        if (!data.isEmpty()) {
            Map<String, String> jvm = new HashMap<>();
            List<Arg> args = getArgs();
            List<Env> envs = new ArrayList<>();

            Map<String, String> dataBackup = new HashMap<>();
            for (String name : data.keySet()) {
                dataBackup.put(name, data.get(name));
            }

            String javaHome = data.remove(JAVA_HOME_KEY);
            if (javaHome != null && !javaHome.isEmpty()) {
                addEnv(envs, JAVA_HOME_KEY, javaHome);
            }

            String newEnvs = data.remove("envs");
            if (newEnvs != null && !newEnvs.isEmpty()) {
                StringTokenizer tokenizer = new StringTokenizer(newEnvs, ",");
                while (tokenizer.hasMoreTokens()) {
                    String env = tokenizer.nextToken();
                    int i = env.indexOf("=");
                    if (i > -1) {
                        String name = env.substring(0, i).trim();
                        if (name.isEmpty()) {
                            continue;
                        }
                        if (JAVA_HOME_KEY.equals(name) && javaHome != null && !javaHome.isEmpty()) {
                            continue;// 修改过Java_home这里不再添加，否正会重复
                        }

                        addEnv(envs, name, env.substring(i + 1).trim());
                    }
                }
            }

            // IP版本属性
            String preferIPv4Stack = data.remove("preferIPv4Stack");
            deleteArg(args, "-Djava.net.preferIPv4Stack=");
            addArg(args, String.format("-Djava.net.preferIPv4Stack=%s", Boolean.parseBoolean(preferIPv4Stack)));

            // . gcLog
            String gcLogEnabled = data.remove("gcLogEnabled");// 用 remove 是为了防止后面被遍历
            if (gcLogEnabled == null || Boolean.parseBoolean(gcLogEnabled)) {
                String gcLog = data.remove("gcLog");// 用 remove 是为了防止后面被遍历
                if (gcLog != null && !gcLog.isEmpty()) {
                    deleteArg(args, "-Xlog");
                    Arg newPro = new Arg();
                    if (isJdk9OrHigher()) {
                        newPro.setSupportedJRE("9+");
                        newPro.setName("-Xlog:gc:" + gcLog);
                    } else {
                        newPro.setSupportedJRE("8");
                        newPro.setName("-Xloggc:" + gcLog);

                    }
                    args.add(newPro);
                }
            } else {
                deleteArg(args, "-Xlog");
            }

            // . jvmLog 先处理开关
            boolean disableLog = false;
            String LogVMOutput = data.remove("LogVMOutput");// 用 remove 是为了防止后面被遍历
            if (LogVMOutput != null) {
                deleteArg(args, "-XX:+UnlockDiagnosticVMOptions");
                deleteArg(args, "-XX:-UnlockDiagnosticVMOptions");
                deleteArg(args, "-XX:+LogVMOutput");
                deleteArg(args, "-XX:-LogVMOutput");
                if (Boolean.parseBoolean(LogVMOutput)) {
                    addArg(args, "-XX:+UnlockDiagnosticVMOptions");
                    addArg(args, "-XX:+LogVMOutput");
                } else {
                    disableLog = true;
                }
            }
            // . jvmLog 再处理日志文件
            if (disableLog) {
                deleteArg(args, "-XX:LogFile");
            } else {
                String LogFile = data.remove("LogFile");// 用 remove 是为了防止后面被遍历
                if (LogFile != null && !LogFile.isEmpty()) {
                    deleteArg(args, "-XX:LogFile");
                    addArg(args, "-XX:LogFile=" + LogFile);
                }
            }

            // GC 具有排他性
            String useGC = data.remove("useGC");
            if (useGC != null) {
                deleteArg(args, "-XX:+Use");
                deleteArg(args, "-XX:-Use");
                if (!useGC.isEmpty()) {
                    addArg(args, "-XX:+" + useGC);
                }
            }

            for (String k : data.keySet()) {
                String v = data.get(k);
                // . -Xms -Xmx -Xmn -Xss
                if (k.startsWith("X")) {
                    deleteArg(args, "-" + k);
                    if (v != null && !v.isEmpty()) {
                        addArg(args, "-" + k + v);
                    }
                    continue;
                }

                // . Print*  Use* 等 ”开关“ 类配置参数
                if (k.startsWith("Print") || k.equals("HeapDumpOnOutOfMemoryError") || k.equals("LogVMOutput")) {
                    deleteArg(args, "-XX:+" + k);
                    deleteArg(args, "-XX:-" + k);
                    if (Boolean.parseBoolean(v)) {
                        addArg(args, "-XX:+" + k);
                    }

                    continue;
                }

                // 剩下的都是带=等号赋值类的
                deleteArg(args, "-XX:" + k);
                if (v != null && !v.isEmpty()) {
                    addArg(args, "-XX:" + k + "=" + v);
                }
            }

            Json json = appContext.getService(Json.class);
            jvm.put("env", json.toJson(envs));
            jvm.put("arg", json.toJson(args));

            getDataStore().updateDataById(request.getId(), jvm);

            // 如果更新成功，保证文件的父目录存在，否则会导致启动无法创建文件而失败
            enSureFileExists(dataBackup);
        }
    }

    private List<Arg> getArgs() throws Exception {
        List<Arg> args = new ArrayList<>();
        Json json = appContext.getService(Json.class);
        for (Map<String, String> data : getDataStore().getAllData()) {
            if (data.remove("type").equals("arg")) {
                args.add(json.fromJson(json.toJson(data), Arg.class));
            }
        }
        return args;
    }

    // 如果更新成功，保证文件的父目录存在，否则会导致启动无法创建文件而失败
    private void enSureFileExists(Map<String, String> map) {
        String[] files = {"gcLog", "HeapDumpPath", "LogFile"};
        for (String s : files) {
            String f = map.get(s);
            if (f != null && !f.isEmpty()) {
                File file = new File(f);
                if (!file.isAbsolute()) {
                    file = new File(InstanceApp.getService(ModuleContext.class).getInstanceDir(), f);
                }
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                }
            }
        }
    }

    private static boolean isJdk9OrHigher() {
        double javaVerson = Double.parseDouble(System.getProperty("java.specification.version"));
        return javaVerson > 1.8;
    }

    private static final Set<String> onlyJava8Config = new HashSet<String>() {{
        add("-XX:+PrintHeapAtGC");
        add("-XX:+PrintGCApplicationStoppedTime");
        add("-XX:+PrintGCApplicationConcurrentTime");
    }};

    private void deleteArg(List<Arg> args, String nameStartsWith) throws Exception {
        for (int i = 0; i < args.size(); i++) {
            Arg arg = args.get(i);
            if (arg.getName().startsWith(nameStartsWith)) {
                args.remove(i);
                break;
            }
        }
    }

    private void addArg(List<Arg> args, String name) {
        Arg arg = new Arg();
        arg.setName(name);
        if (onlyJava8Config.contains(name)) {
            arg.setSupportedJRE("8");
        }
        args.add(arg);
    }

    private void addEnv(List<Env> envs, String name, String value) {
        Env env = new Env();
        env.setName(name);
        env.setValue(value);
        envs.add(env);
    }

    @ModelAction(
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
        show(request, response);
    }

    @ModelAction(
            name = {"查看", "en:Show"},
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request, Response response) throws Exception {
        Map<String, String> jvm = new HashMap<>();
        Map<String, String> envMap = new LinkedHashMap<>();
        for (Map<String, String> element : getDataStore().getAllData()) {
            String type = element.remove("type");
            if ("arg".equals(type) && Boolean.parseBoolean(element.getOrDefault("enabled", "true"))) {
                String argKV = element.get("name");
                // 1. -XX:
                if (argKV.startsWith("-XX:")) {
                    String temp = argKV.substring(4);
                    if (temp.charAt(0) == '+') {
                        String guessGC = temp.substring(1);
                        if (guessGC.startsWith("Use")) {
                            jvm.put("useGC", guessGC);
                        } else {
                            jvm.put(temp.substring(1), "true");
                        }
                    } else if (temp.charAt(0) == '-') {
                        jvm.put(temp.substring(1), "false");
                    } else {
                        int i = temp.indexOf("=");
                        jvm.put(temp.substring(0, i), temp.substring(i + 1));
                    }
                    continue;
                }

                // 2. -Xms -Xmx -Xmn -Xss
                if (argKV.startsWith("-X")) {
                    if (argKV.startsWith("-Xlog")) { // jdk8: -Xloggc:xx; jdk9: -Xlog:gc:xxxx;
                        int i = argKV.lastIndexOf(":");
                        jvm.put("gcLogEnabled", "true");
                        jvm.put("gcLog", argKV.substring(i + 1));
                    } else {
                        jvm.put(argKV.substring(1, 4), argKV.substring(4));
                    }
                    continue;
                }

                boolean preferIPv4Stack = false;
                if (argKV.startsWith("-D")) {
                    if (argKV.startsWith("-Djava.net.preferIPv4Stack")) {
                        int index = argKV.indexOf("=");
                        preferIPv4Stack = Boolean.parseBoolean(argKV.substring(index + 1));
                    }
                }
                jvm.put("preferIPv4Stack", String.valueOf(preferIPv4Stack));
            } else if ("env".equals(type)) {
                if (Boolean.parseBoolean(element.getOrDefault("enabled", "true"))) {
                    String name = element.get("name");
                    String value = element.get("value");
                    if (JAVA_HOME_KEY.equals(name)) {
                        jvm.put(JAVA_HOME_KEY, value);
                    } else {
                        envMap.put(name, value);
                    }
                }
            }
        }

        // 处理环境变量
        StringJoiner sj = new StringJoiner(",");

        for (Map.Entry<String, String> entry : envMap.entrySet()) {
            sj.add(entry.getKey() + "=" + entry.getValue());
        }

        if (Boolean.parseBoolean(jvm.getOrDefault("LogVMOutput", "false"))) {
            jvm.put("LogFile", jvm.getOrDefault("LogFile", "logs/jvm/jvm.log"));
        }
        if (Boolean.parseBoolean(jvm.getOrDefault("gcLogEnabled", "false"))) {
            jvm.put("gcLog", jvm.getOrDefault("gcLog", "logs/gc/gc.log"));
        }
        if (Boolean.parseBoolean(jvm.getOrDefault("HeapDumpOnOutOfMemoryError", "false"))) {
            jvm.put("HeapDumpPath", jvm.getOrDefault("HeapDumpPath", "logs/heap_${QZ_TimeStamp}.hprof"));
        }

        jvm.put("envs", sj.toString());
        response.addData(jvm);
    }

    private boolean validate(Request request, Response response) throws Exception {
        Map<String, String> parameters = request.getParameters();
        for (String fieldName : parameters.keySet()) {
            String newValue = parameters.get(fieldName);
            if (newValue != null && !newValue.isEmpty()) {
                String[] SIZE_KEYS = {"Xms", "Xmx", "MetaspaceSize", "MaxMetaspaceSize", "Xss", "MaxDirectMemorySize", "Xmn"};
                for (String key : SIZE_KEYS) {
                    if (key.equals(fieldName)) {
                        if (!newValue.matches("\\d+([mMgG])")) {
                            ModelFieldInfo modelFieldInfo = InstanceApp.getService(Deployer.class).getApp(DeployerConstants.INSTANCE_APP_NAME).getAppInfo().getModelInfo(request.getModel()).getModelFieldInfo(fieldName);
                            response.setSuccess(false);
                            response.setMsg(String.format(this.appContext.getI18n(request.getLang(), "validator.arg.memory.invalid"), I18nTool.retrieveI18n(modelFieldInfo.getName()).get(request.getLang())));
                            return false;
                        }
                    }
                }

                for (int i = 0; i < sizeKeyPairs.length; i += 2) {
                    if (fieldName.equals(sizeKeyPairs[i])) {
                        String msg = checkSize(fieldName, newValue, sizeKeyPairs[i + 1], request);
                        if (msg != null) {
                            response.setSuccess(false);
                            response.setMsg(msg);
                            return false;
                        }
                    }
                }

                List<String> validateFields = Arrays.asList("HeapDumpPath", "LogFile", "gcLog");
                for (String field : validateFields) {
                    if (field.equals(fieldName)) {
                        if (field.equals("HeapDumpPath")) {
                            if (!newValue.endsWith(".hprof")) {
                                response.setSuccess(false);
                                response.setMsg(this.appContext.getI18n(request.getLang(), "validator.check.heapDumpPath"));
                                return false;
                            }
                        } else {
                            if (!(newValue.endsWith(".log") || newValue.endsWith(".txt"))) {
                                response.setSuccess(false);
                                response.setMsg(this.appContext.getI18n(request.getLang(), "validator.check.logFile"));
                                return false;
                            }
                        }

                        String flag = "${QZ_TimeStamp}";
                        if (newValue.contains(flag)) {
                            String[] split = newValue.replace("\\", "/").split("/");
                            for (int i = 0; i < split.length - 1; i++) {
                                if (split[i].contains(flag)) {
                                    response.setSuccess(false);
                                    response.setMsg(this.appContext.getI18n(request.getLang(), "validator.check.timeFlag"));
                                    return false;
                                }
                            }

                            newValue = newValue.replace(flag, "");
                        }
                        if (!checkFilePath(newValue)) {
                            response.setSuccess(false);
                            response.setMsg(this.appContext.getI18n(request.getLang(), "validator.check.filePath"));
                            return false;
                        }
                    }
                }

                if ("envs".equals(fieldName)) {
                    String[] envArr = newValue.split(DATA_SEPARATOR);
                    for (String env : envArr) {
                        String[] kv = env.split("=");
                        if (kv[0].equals(JAVA_HOME_KEY)) {
                            response.setSuccess(false);
                            response.setMsg(this.appContext.getI18n(request.getLang(), "validator.env.notJava"));
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private static boolean checkFilePath(String newValue) {
        String[] illegalCollections = {"|", "&", "~", "../", "./", ":", "*", "?", "\"", "'", "<", ">", "(", ")", "[", "]", "{", "}", "^", " "};
        for (String illegalCollection : illegalCollections) {
            if (newValue.contains(illegalCollection)) {
                return false;
            }
        }
        return true;
    }

    private String checkSize(String key, String newValue, String maxKey, Request request) {
        String max = request.getParameter(maxKey);
        if (max != null && !max.isEmpty()) {
            if (!max.matches("\\d+([mMgG])")) {
                ModelFieldInfo modelFieldInfo = InstanceApp.getService(Deployer.class).getApp(DeployerConstants.INSTANCE_APP_NAME).getAppInfo().getModelInfo(request.getModel()).getModelFieldInfo(maxKey);
                return String.format(this.appContext.getI18n(request.getLang(), "validator.arg.memory.union.invalid"), I18nTool.retrieveI18n(modelFieldInfo.getName()).get(request.getLang()));
            }

            if (getValueAsByte(newValue) > getValueAsByte(max)) {
                String msg = this.appContext.getI18n(request.getLang(), "validator.larger.cannot");
                ModelInfo modelInfo = InstanceApp.getService(Deployer.class).getApp(DeployerConstants.INSTANCE_APP_NAME).getAppInfo().getModelInfo(request.getModel());
                ModelFieldInfo keyFieldInfo = modelInfo.getModelFieldInfo(key);
                ModelFieldInfo maxKeyFieldInfo = modelInfo.getModelFieldInfo(maxKey);
                String keyName = I18nTool.retrieveI18n(keyFieldInfo.getName()).get(request.getLang());
                String maxKeyName = I18nTool.retrieveI18n(maxKeyFieldInfo.getName()).get(request.getLang());
                return String.format(msg, keyName, maxKeyName);
            }
        }
        return null;
    }

    private long getValueAsByte(String val) {
        String unit = val.substring(val.length() - 1);
        int level = -1;
        if (unit.equalsIgnoreCase("K")) {
            level = 1024;
        } else if (unit.equalsIgnoreCase("M")) {
            level = 1024 * 1024;
        } else if (unit.equalsIgnoreCase("G")) {
            level = 1024 * 1024 * 1024;
        } else {
            if (Character.isDigit(unit.charAt(0))) {
                level = 1;
            }
        }
        if (level < 1) {
            throw new IllegalArgumentException(val + ":" + level);
        }

        if (level == 1) {
            return Long.parseLong(val);
        } else {
            return Long.parseLong(val.substring(0, val.length() - 1)) * level;
        }
    }


    @Override
    public DataStore getDataStore() {
        return jvmDataStore;
    }

    private final JvmDataStore jvmDataStore = new JvmDataStore();

    private class JvmDataStore implements DataStore {
        @Override
        public List<Map<String, String>> getAllData() throws Exception {
            Jvm jvm = InstanceApp.getService(Config.class).getJvm();
            if (jvm == null) {
                return null;
            }
            List<Arg> args = jvm.getArg();
            List<Map<String, String>> dataList = new ArrayList<>();
            for (Arg arg : args) {
                Map<String, String> argMap = Utils.getPropertiesFromObj(arg);
                argMap.put("type", "arg");
                dataList.add(argMap);
            }
            List<Env> envs = jvm.getEnv();
            for (Env env : envs) {
                Map<String, String> envMap = Utils.getPropertiesFromObj(env);
                envMap.put("type", "env");
                dataList.add(envMap);
            }

            return dataList;
        }

        @Override
        public void addData(String id, Map<String, String> data) throws Exception {
            throw new RuntimeException("No Support.");
        }

        @Override
        public void updateDataById(String id, Map<String, String> data) throws Exception {
            Json json = appContext.getService(Json.class);
            Config config = InstanceApp.getService(Config.class);
            Jvm jvm = config.getJvm();
            List<Arg> oldArgs = jvm.getArg();
            for (Arg oldArg : oldArgs) {
                config.deleteArg(oldArg.getName());
            }
            List<Map<String, String>> args = json.fromJson(data.get("arg"), List.class);
            for (Map<String, String> map : args) {
                Arg arg = new Arg();
                Utils.setPropertiesToObj(arg, map);
                config.addArg(arg);
            }

            List<Env> oldEnvs = jvm.getEnv();
            for (Env oldEnv : oldEnvs) {
                config.deleteEnv(oldEnv.getName());
            }
            List<Map<String, String>> envs = json.fromJson(data.get("env"), List.class);
            for (Map<String, String> map : envs) {
                Env env = new Env();
                Utils.setPropertiesToObj(env, map);
                config.addEnv(env);
            }
        }

        @Override
        public void deleteDataById(String id) throws Exception {
            throw new RuntimeException("No Support.");
        }
    }
}
