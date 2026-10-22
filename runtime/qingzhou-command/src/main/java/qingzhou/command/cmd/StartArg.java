package qingzhou.command.cmd;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import qingzhou.command.Processor;
import qingzhou.config.impl.Config;

public class StartArg extends Processor {
    private File instanceDir;

    @Override
    public String name() {
        return "start-arg";
    }

    @Override
    public String info() {
        return "Run the instance. Tip: Put the instance (instance) name at the end of the command.";
    }

    @Override
    public String[] classpath() {
        return new String[]{"modules/qingzhou-config.jar", "components/qingzhou-json.jar"};
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        instanceDir = initInstance(args);
        if (instanceDir == null) return;

        Path configFile = Paths.get(instanceDir.getAbsolutePath(), "conf", "qingzhou.properties");
        Map<String, String> properties = parseConfig(configFile);
        List<String> jvmConfig = getJvmArgs(properties);

        String javaCmd = Paths.get(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java").toString();
        // 构造启动命令
        StringBuilder startCmd = new StringBuilder(wrap(javaCmd));
        for (String arg : jvmConfig) {
            startCmd.append(" ").append(arg);
        }

        // 通过标准输出传递给 standalone 脚本
        System.out.print(startCmd);
    }

    private List<String> getJvmArgs(Map<String, String> properties) {
        List<String> commands = new ArrayList<>();

        String arg = properties.get("jvm.arg");
        if (arg != null) {
            for (String s : arg.trim().split("\\s+")) {
                s = s.trim();
                if (s.isEmpty()) continue;
                if (isForbiddenJvmArg(s) && !isAllowedJvmArg(s, properties.get("jvm.arg.allowed"))) {
                    logStartInfo("ignore forbidden jvm arg: " + s);
                    continue;
                }
                // jvm.arg 经 sh 的 eval 执行，`$()`/反引号等会被 shell 展开构成命令注入；
                // 引号会破坏 eval 的命令结构；Windows 下 bat 的 %VAR% 展开构成环境变量注入，一并拒绝
                if (containsShellMeta(s) || s.contains("\"") || (windows && s.contains("%"))) {
                    logStartInfo("ignore jvm arg with unsafe characters: " + s);
                    continue;
                }
                commands.add(s);
            }
        }

        commands.add(wrap("-Duser.dir=" + instanceDir.getAbsolutePath()));
        commands.add(wrap("-Dqingzhou.instance=" + instanceDir.getAbsolutePath()));
        commands.add(wrap("-Dqingzhou.version=" + getLibDir().getName().substring("version".length())));

        commands.add("-classpath");
        File runtimeDir = new File(getLibDir(), "runtime");
        File[] runtimeFiles = runtimeDir.listFiles(f -> !f.isDirectory());
        if (runtimeFiles == null) throw new IllegalStateException("Runtime libs not found: " + runtimeDir);

        commands.add(wrap(Arrays.stream(runtimeFiles)
                .map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator))));
        commands.add("qingzhou.engine.QingzhouMain");
        return commands;
    }

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 输出走 stderr：stdout 是传给 standalone 脚本的启动命令契约，告警混入会破坏 eval 内容
    protected void logStartInfo(String msg) {
        System.err.println(LOG_TIME.format(LocalDateTime.now()) + " >>> " + msg);
    }

    // 整体加引号，避免启动参数被 shell 按空白拆分；启动命令由 sh 的 eval 与 bat 的 %startCmd% 直接执行，
    // 两种环境的转义规则不同，无法用统一转义保证安全，故直接拒绝会破坏命令结构或可被展开执行的值
    private String wrap(String str) {
        String unsafe = windows ? "\"%^!" : "\"$`\\"; // 双引号内仍具特殊语义：sh 的 \ 与 bat 的 %VAR%、^、!
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\n' || c == '\r' || unsafe.indexOf(c) >= 0) {
                throw new IllegalStateException("Illegal character in start arg: " + str);
            }
        }
        return "\"" + str + "\"";
    }

    private static boolean isForbiddenJvmArg(String arg) {
        return arg.startsWith("-agentlib") || arg.startsWith("-javaagent")
                || arg.startsWith("-agentpath") || arg.startsWith("-Xrun");
    }

    // sh 经 eval 执行启动命令：以下字符可被 shell 当作语法/展开/重定向，必须拒之门外
    private static boolean containsShellMeta(String arg) {
        for (int i = 0; i < arg.length(); i++) {
            char c = arg.charAt(i);
            if (c == '$' || c == '`' || c == '(' || c == ')' || c == ';' || c == '&'
                    || c == '|' || c == '<' || c == '>' || c == '\n' || c == '\r') {
                return true;
            }
        }
        return false;
    }

    // 默认拒绝上述参数；确有需要（如接入 APM 探针）时，可用 jvm.arg.allowed 配置前缀显式放行
    private static boolean isAllowedJvmArg(String arg, String allowedList) {
        if (allowedList == null) return false;

        for (String allowed : allowedList.split(",")) {
            String prefix = allowed.trim();
            if (!prefix.isEmpty() && arg.startsWith(prefix)) return true;
        }
        return false;
    }

    private Map<String, String> parseConfig(Path configFile) throws Exception {
        return Config.parse(new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8));
    }
}
