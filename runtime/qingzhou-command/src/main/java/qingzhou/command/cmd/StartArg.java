package qingzhou.command.cmd;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
        Properties properties = parseConfig(configFile);
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

    private List<String> getJvmArgs(Properties properties) {
        List<String> commands = new ArrayList<>();

        String arg = properties.getProperty("jvm.arg");
        if (arg != null) {
            String[] args = arg.trim().split("\\s+");
            for (String s : args) {
                s = s.trim();
                if (!s.isEmpty()) {
                    commands.add(s);
                }
            }
        }

        commands.add(wrap("-Duser.dir=" + instanceDir.getAbsolutePath()));
        commands.add(wrap("-Dqingzhou.instance=" + instanceDir.getAbsolutePath()));
        commands.add(wrap("-Dqingzhou.version=" + getLibDir().getName().substring("version".length())));

        commands.add("-classpath");
        commands.add(wrap(Arrays.stream(Objects.requireNonNull(new File(getLibDir(), "runtime").listFiles(f -> !f.isDirectory())))
                .map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator))));
        commands.add("qingzhou.engine.QingzhouMain");
        return commands;
    }

    private String wrap(String str) { // 兼容启动参数中包含中文空格
        return "\"" + str + "\"";
    }

    private Properties parseConfig(Path configFile) throws Exception {
        URL configJar = Paths.get(getLibDir().getAbsolutePath(), "modules", "qingzhou-config.jar").toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{configJar})) {
            Class<?> configClass = loader.loadClass("qingzhou.config.impl.Config");
            Method parseConfig = configClass.getMethod("parseConfig", Path.class);
            return (Properties) parseConfig.invoke(null, configFile);
        }
    }
}
