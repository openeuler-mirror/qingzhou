package qingzhou.command.cmd;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import qingzhou.command.Processor;

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
    public void doCommandLine(String[] args) throws Exception {
        String instance = args.length > 0 ? args[0] : "default";
        instanceDir = initInstance(instance);
        if (instanceDir == null) return;

        Path configFile = Paths.get(instanceDir.getAbsolutePath(), "conf", "qingzhou.properties");
        Map<String, String> properties = parseConfig(configFile);
        List<String> jvmConfig = getJvmArgs(properties);

        // prepare javaCmd
        String javaCmd = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        // 构造启动命令
        StringBuilder startCmd = new StringBuilder("\"" + javaCmd + "\"");
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
                    log("ignore forbidden jvm arg: " + s);
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

    private String wrap(String str) { // 兼容启动参数中包含中文空格
        // 启动命令由 sh 的 eval 与 bat 的 %startCmd% 直接执行，两种环境的引号转义规则不同，
        // 无法用统一的转义保证安全，故直接拒绝含引号的值，避免静默破坏命令结构。
        if (str.contains("\"")) throw new IllegalStateException("Illegal quote character in start arg: " + str);
        return "\"" + str + "\"";
    }

    private static boolean isForbiddenJvmArg(String arg) {
        return arg.startsWith("-agentlib") || arg.startsWith("-javaagent")
                || arg.startsWith("-agentpath") || arg.startsWith("-Xrun");
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
        URL configJar = Paths.get(getLibDir().getAbsolutePath(), "modules", "qingzhou-config.jar").toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{configJar})) {
            Class<?> configClass = loader.loadClass("qingzhou.config.impl.Config");
            Method parseConfig = configClass.getMethod("parse", String.class);
            String configText = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
            return (Map<String, String>) parseConfig.invoke(null, configText);
        }
    }
}
