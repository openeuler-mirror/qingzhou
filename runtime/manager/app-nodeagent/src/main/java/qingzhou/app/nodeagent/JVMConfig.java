package qingzhou.app.nodeagent;

import qingzhou.api.*;
import qingzhou.api.type.Editable;
import qingzhou.framework.util.StringUtil;

import java.util.Arrays;
import java.util.List;

@Model(name = JVMConfig.MODEL_NAME_jvmconfig, icon = "coffee", entryAction = Editable.ACTION_NAME_EDIT, nameI18n = {"JVM 配置", "en:JVM Configuration"}, infoI18n = {"配置运行 Qingzhou 应用服务器的 JVM 属性。", "en:Configure the JVM properties of the server running Qingzhou applications."})
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
    public void init() {
        AppContext appContext = getAppContext();
        appContext.addI18n("validator.javaHome.notDir", new String[]{"Java 路径不是一个有效的目录", "en:The Java path is not a valid directory"});
        appContext.addI18n("validator.javaHome.notValid", new String[]{"Java 路径无效", "en:Invalid Java path"});
        appContext.addI18n("validator.javaHome.version.notSupport", new String[]{"Java 版本不支持, 需要 Java 8 或以上版本", "en:Java version is not supported and requires Java 8 or later"});

        appContext.addI18n("validator.env.notJava", new String[]{"请不要通过这里设置 JAVA_HOME 环境变量，您可使用”Java 路径“参数来设置",
                "en:Please do not set the JAVA_HOME environment variable here, you can set it using \"JAVA_HOME\" Parameter"});
        appContext.addI18n("validator.check.filePath", new String[]{"文件路径不支持以\\或者/结尾，不支持包含特殊字符和空格", "en:The file path cannot end with \\ or /, and cannot contain special characters or Spaces"});
        appContext.addI18n("validator.check.heapDumpPath", new String[]{"堆转储文件仅支持以.hprof结束", "en:Heap dumps are only supported for files ending in .hprof"});
        appContext.addI18n("validator.check.logFile", new String[]{"日志文件只支持以.log或.txt结束", "en:Log files only support ending in .log or .txt"});
        appContext.addI18n("validator.check.timeFlag", new String[]{"时间戳只能出现在文件名上，而不能在文件夹上", "en:The timestamp can only appear on the file name, not on the folder"});
    }

    @ModelField(group = group_memory, nameI18n = {"初始堆内存", "en:Initial Heap Size"}, infoI18n = {"设置初始堆内存大小 (-Xms)。", "en:Set the initial heap size (-Xms)."})
    public String Xms;

    @ModelField(group = group_memory, nameI18n = {"最大堆内存", "en:Max Heap Size"}, infoI18n = {"设置最大堆内存大小 (-Xmx)。", "en:Set the max heap size (-Xmx)."})
    public String Xmx;

    @ModelField(group = group_memory, nameI18n = {"新生代大小", "en:Xmn Size"}, infoI18n = {"设置新生代大小 (-Xmn)。", "en:Set the new generation size (-Xmn)."})
    public String Xmn;

    @ModelField(group = group_memory, effectiveWhen = "Xmn=", min = 1, max = 100, type = FieldType.number, nameI18n = {"老年代比率", "en:New Ratio"}, infoI18n = {"设置新生代和老年代的大小比率，通俗地讲即老年代比新生代的倍数 (-XX:NewRatio)。", "en:Set the size ratio of the new generation and the old era, which is colloquially speaking, that is, the multiple of the old era to the new generation (-XX:NewRatio)."})
    public Integer NewRatio;

    @ModelField(group = group_memory, min = 1, max = 100, type = FieldType.number, nameI18n = {"Eden 区比率", "en:Survivor Ratio"}, infoI18n = {"设置新生代中 Eden 区域和 Survivor 区域（From 幸存区或 To 幸存区）的比率 (-XX:SurvivorRatio)。", "en:Sets the ratio of the Eden zone to the Survivor zone (From Survivor Zone or To Survivor Zone) in the new generation (-XX:SurvivorRatio)."})
    public Integer SurvivorRatio;

    @ModelField(group = group_memory, nameI18n = {"初始元空间", "en:MetaspaceSize"}, infoI18n = {"设置初始元空间的大小 (-XX:MetaspaceSize)。", "en:Set the initial Metaspace size (-XX:MetaspaceSize)."})
    public String MetaspaceSize;

    @ModelField(group = group_memory, nameI18n = {"最大元空间", "en:MaxMetaspaceSize"}, infoI18n = {"设置最大元空间的大小 (-XX:MaxMetaspaceSize)。", "en:Set the max metaspace size (-XX:MaxMetaspaceSize)."})
    public String MaxMetaspaceSize;

    @ModelField(group = group_memory, nameI18n = {"线程栈大小", "en:Stack Size"}, infoI18n = {"设置线程栈大小的最大值 (-Xss)。", "en:Set the max thread stack size (-Xss)."})
    public String Xss;

    @ModelField(group = group_memory, nameI18n = {"堆外内存", "en:MaxDirectMemorySize"}, infoI18n = {"设置最大堆外内存，当 Direct ByteBuffer 分配的堆外内存达到该大小后，即触发 Full GC (-XX:MaxDirectMemorySize)。", "en:Set the maximum out-of-heap memory. When the out-of-heap memory allocated by Direct ByteBuffer reaches this size, Full GC is triggered (-XX:MaxDirectMemorySize)."})
    public String MaxDirectMemorySize;

    @ModelField(group = group_gc, type = FieldType.radio, nameI18n = {"垃圾回收器", "en:GarbageCollector"}, infoI18n = {"设置 JVM 回收内存使用的垃圾回收器。", "en:Set up the garbage collector used by the JVM to reclaim memory."})
    public String useGC;

    @ModelField(group = group_gc, effectiveWhen = "useGC=UseParallelGC|useGC=UseConcMarkSweepGC", min = 1, max = 100, type = FieldType.number, nameI18n = {"并行收集线程数", "en:ParallelGCThreads"}, infoI18n = {"设置并行收集器收集时使用的CPU数，即并行收集线程数 (-XX:ParallelGCThreads)。", "en:Set the number of CPUs used by the parallel collector collection, that is, the number of parallel collection threads (-XX:ParallelGCThreads)."})
    public Integer ParallelGCThreads;
    @ModelField(group = group_gc, effectiveWhen = "useGC=UseParallelGC", min = 1, max = 100, type = FieldType.number, nameI18n = {"最大暂停时间", "en:MaxGCPauseMillis"}, infoI18n = {"每次 GC 最大的停顿毫秒数，VM 将调整 Java 堆大小和其他与 GC 相关的参数，以使 GC 引起的暂停时间短于该毫秒，尽可能地保证内存回收花费时间不超过设定值（-XX:MaxGCPauseMillis）。", "en:For each GC maximum pause milliseconds, the VM will adjust the Java heap size and other GC-related parameters so that the pause caused by GC is shorter than that millisecond, ensuring that memory reclamation takes no longer than the set value as much as possible (-XX:MaxGCPauseMillis)."})
    public Integer MaxGCPauseMillis;
    @ModelField(group = group_gc, effectiveWhen = "useGC=UseParallelGC", min = 1, max = 100, type = FieldType.number, nameI18n = {"回收时间占比", "en:GCTimeRatio"}, infoI18n = {"设置垃圾回收时间占程序运行时间的百分比 (-XX:GCTimeRatio)。", "en:Sets the garbage collection time as a percentage of the program running time (-XX:GCTimeRatio)."})
    public Integer GCTimeRatio;

    @ModelField(group = group_gc, type = FieldType.bool, nameI18n = {"记录 GC 日志", "en:Print GC "}, infoI18n = {"设置是否记录 JVM 的 GC 日志。", "en:Sets whether to record GC logs for the JVM."})
    public boolean gcLogEnabled;
    @ModelField(group = group_gc, effectiveWhen = "gcLogEnabled=true", type = FieldType.bool, nameI18n = {"记录细节", "en:GC Details"}, infoI18n = {"设置是否记录 GC 的详细信息 (-XX:+PrintGCDetails)。", "en:Set whether GC details are logged (-XX:+PrintGCDetails)."})
    public boolean PrintGCDetails;
    @ModelField(group = group_gc, effectiveWhen = "gcLogEnabled=true", type = FieldType.bool, nameI18n = {"记录系统停顿时间", "en:Application StoppedTime"}, infoI18n = {"是否在 GC 日志中记录系统停顿时间。仅适用于 java8 (-XX:+PrintGCApplicationStoppedTime)。", "en:Whether application StoppedTime are recorded in GC logs, only available for java8 (-XX:+PrintGCApplicationStoppedTime)."})
    public boolean PrintGCApplicationStoppedTime;
    @ModelField(group = group_gc, effectiveWhen = "gcLogEnabled=true", type = FieldType.bool, nameI18n = {"记录系统执行时间", "en:Application ConcurrentTime"}, infoI18n = {"是否在 GC 日志中记录系统执行时间。仅适用于 java8 (-XX:+PrintGCApplicationConcurrentTime)。", "en:Whether application ConcurrentTime are recorded in GC logs, only available for java8 (-XX:+PrintGCApplicationConcurrentTime)."})
    public boolean PrintGCApplicationConcurrentTime;
    @ModelField(group = group_gc, effectiveWhen = "gcLogEnabled=true", type = FieldType.bool, nameI18n = {"记录堆信息", "en:Print Heap"}, infoI18n = {"是否在 GC 日志中记录堆信息，仅适用于java8 (-XX:+PrintHeapAtGC)。", "en:Whether heap information is recorded in the GC log, only available for java8 (-XX:+PrintHeapAtGC)."})
    public boolean PrintHeapAtGC;
    @ModelField(group = group_gc, effectiveWhen = "gcLogEnabled=true", required = true, nameI18n = {"GC 日志文件", "en:GC Log Path"},
            skipCharacterCheck = "${}",
            infoI18n = {"设置 GC 日志的存储位置。注：路径可以是绝对路径，也可以是相对于 Qingzhou 实例目录的相对路径，同时可使用 ${TW_TimeStamp} 变量添加 Qingzhou 启动时间戳 (-Xlog)。",
                    "en:Set the storage location for GC logs. Note: The path can be absolute or relative to the Qingzhou instance directory, and the Qingzhou startup timestamp can be added using the ${TW_TimeStamp} variable (-Xlog)."})
    public String gcLog = "logs/gc/gc.log";

    @ModelField(group = group_heapDump, type = FieldType.bool, nameI18n = {"开启堆转储", "en:Heap Dump"}, infoI18n = {"当 JVM 发生 OOM 时，自动生成 DUMP 文件，文件位置由【堆转储文件】选项指定 (-XX:+HeapDumpOnOutOfMemoryError)。", "en:When OOM occurs in JVM, a dump file is automatically generated, and the file location is specified by the [Heap Dump Path] option (-XX:+HeapDumpOnOutOfMemoryError)."})
    public boolean HeapDumpOnOutOfMemoryError;
    @ModelField(group = group_heapDump, effectiveWhen = "HeapDumpOnOutOfMemoryError=true", required = true,
            skipCharacterCheck = "${}",
            nameI18n = {"堆转储文件", "en:Heap Dump Path"}, infoI18n = {"设置 JVM 发生 OOM 时，自动生成 DUMP 文件的路径。注：路径可以是绝对路径，也可以是相对于 Qingzhou 域目录的相对路径，同时可使用 ${TW_TimeStamp} 变量添加 Qingzhou 启动时间戳 (-XX:HeapDumpPath)。",
            "en:Set the path to the JVM to automatically generate a dump file when OOM occurs. Note: The path can be absolute or relative to the Qingzhou instance directory, and the Qingzhou startup timestamp can be added using the ${TW_TimeStamp} variable (-XX:HeapDumpPath)."})
    public String HeapDumpPath = "logs/heap_${TW_TimeStamp}.hprof";

    @ModelField(group = group_jvmLog, type = FieldType.bool, nameI18n = {"记录 JVM 日志", "en:Log VM Output"}, infoI18n = {"设置是否记录 JVM 日志 (-XX:+LogVMOutput)。", "en:Set whether to log JVM logs (-XX:+LogVMOutput)."})
    public boolean LogVMOutput;
    @ModelField(group = group_jvmLog, effectiveWhen = "LogVMOutput=true", required = true,
            nameI18n = {"JVM 日志文件", "en:JVM Log Path"},
            skipCharacterCheck = "${}",
            infoI18n = {"设置 JVM 日志的存储位置。注：路径可以是绝对路径，也可以是相对于 Qingzhou 域目录的相对路径，同时可使用 ${TW_TimeStamp} 变量添加 Qingzhou 启动时间戳 (-XX:LogFile)。",
                    "en:Set the location where JVM logs are stored. Note: The path can be absolute or relative to the Qingzhou instance directory, and the Qingzhou startup timestamp can be added using the ${TW_TimeStamp} variable (-XX:LogFile)."})
    public String LogFile = "logs/jvm/jvm.log";

    @ModelField(group = group_environment,
            nameI18n = {"Java 路径", "en:Java Home"},
            infoI18n = {"设置运行 Qingzhou 所需要的 Java 路径。", "en:Set the Java path required to run Qingzhou."})
    public String JAVA_HOME;
    @ModelField(group = group_environment, type = FieldType.kv, nameI18n = {"环境变量", "en:Environments"},
            skipCharacterCheck = "%",
            infoI18n = {"设置应用程序所需要的环境变量。", "en:Set the environment variables required by the application."})
    public String envs;

    @ModelField(group = group_IP, type = FieldType.bool,
            nameI18n = {"首选 IPv4", "en:Prefer IPv4"},
            infoI18n = {"在支持 IPv4 映射地址的 IPv6 网络协议栈中，首选使用 IPv4 协议栈 (-Djava.net.preferIPv4Stack)。",
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

    @Override
    public Options options(Request request, String fieldName) {
        if (fieldName.equals("useGC")) {
            return Options.of(
                    Option.of("", new String[]{"JVM 默认", "en:JVM Default"}),
                    Option.of("UseSerialGC", new String[]{"串行", "en:Serial"}),
                    Option.of("UseParallelGC", new String[]{"并行", "en:Parallel"}),
                    Option.of("UseConcMarkSweepGC", new String[]{"并发", "en:ConcMarkSweep"}),
                    Option.of("UseG1GC", new String[]{"G1", "en:UseG1GC"})
            );
        }

        return super.options(request, fieldName);
    }

    @Override
    public String validate(Request request, String fieldName) {
        String newValue = request.getParameter(fieldName);
        if (StringUtil.isBlank(newValue)) return null;


        String[] SIZE_KEYS = {"Xms", "Xmx", "MetaspaceSize", "MaxMetaspaceSize", "Xss", "MaxDirectMemorySize", "Xmn"};
        for (String key : SIZE_KEYS) {
            if (key.equals(fieldName)) {
                if (!newValue.matches("\\d+([mMgG])")) {
                    return getI18nString(request, "validator.arg.memory.invalid");
                }
                break;
            }
        }

        for (int i = 0; i < sizeKeyPairs.length; i += 2) {
            if (fieldName.equals(sizeKeyPairs[i])) {
                String msg = checkSize(newValue, sizeKeyPairs[i + 1], request);
                if (msg != null) {
                    return msg;
                }
            }
        }
        List<String> validateFields = Arrays.asList("HeapDumpPath", "LogFile", "gcLog");
        for (String field : validateFields) {
            if (field.equals(fieldName)) {
                if (field.equals("HeapDumpPath")) {
                    if (!newValue.endsWith(".hprof")) {
                        return getI18nString(request, "validator.check.heapDumpPath");
                    }
                } else {
                    if (!(newValue.endsWith(".log") || newValue.endsWith(".txt"))) {
                        return getI18nString(request, "validator.check.logFile");
                    }
                }

                String flag = "${TW_TimeStamp}";
                if (newValue.contains(flag)) {
                    String[] split = newValue.replace("\\", "/").split("/");
                    for (int i = 0; i < split.length - 1; i++) {
                        if (split[i].contains(flag)) {
                            return getI18nString(request, "validator.check.timeFlag");
                        }
                    }

                    newValue = newValue.replace(flag, "");
                }
                if (!checkFilePath(newValue)) {
                    return getI18nString(request, "validator.check.filePath");
                }
            }
        }

        if ("envs".equals(fieldName)) {
            String[] envArr = newValue.split(DATA_SEPARATOR);
            for (String env : envArr) {
                String[] kv = env.split("=");
                if (kv[0].equals(JAVA_HOME_KEY)) {
                    return getI18nString(request, "validator.env.notJava");
                }
            }
        }

        return super.validate(request, fieldName);
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

    private String checkSize(String newValue, String maxKey, Request request) {
        String max = request.getParameter(maxKey);
        if (StringUtil.notBlank(max)) {
            if (!max.matches("\\d+([mMgG])")) {
                return String.format(getI18nString(request, "validator.arg.memory.union.invalid"), getI18nString(request, "model.field." + MODEL_NAME_jvmconfig + "." + maxKey));
            }

            if (getValueAsByte(newValue) > getValueAsByte(max)) {
                String msg = getI18nString(request, "validator.larger.cannot");
                return String.format(msg, getI18nString(request, "model.field." + MODEL_NAME_jvmconfig + "." + maxKey));
            }
        }
        return null;
    }

    private String getI18nString(Request request, String key) {
        return getAppContext().getAppMetadata().getI18n(request.getI18nLang(), key);
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
}
