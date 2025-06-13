package qingzhou.command.instance;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import qingzhou.command.CommandUtil;

public class ConfigTool {
    private final File instanceDir;
    private final List<String> commands = new ArrayList<>();
    private final Map<String, String> envs = new HashMap<>();

    public ConfigTool(File instanceDir) throws Exception {
        this.instanceDir = instanceDir;
        initConfig();
    }

    public String getJavaHomeEnv() {
        return envs.get("JAVA_HOME");
    }

    Map<String, String> environment() {
        return envs;
    }

    public List<String> command() {
        return commands;
    }

    private void initConfig() throws Exception {
        Map jvm = parseFileConfig();

        List<Map> envs = (List<Map>) jvm.get("env");
        for (Map env : envs) {
            if (env.get("enabled") != null && !Boolean.parseBoolean(String.valueOf(env.get("enabled")))) continue;

            this.envs.put(String.valueOf(env.get("name")),
                    String.valueOf(env.get("value")));
        }

        List<Map> args = (List<Map>) jvm.get("arg");
        for (Map arg : args) {
            if (arg.get("enabled") != null && !Boolean.parseBoolean(String.valueOf(arg.get("enabled")))) continue;

            if (CommandUtil.isWindows()) {
                if (Boolean.parseBoolean(String.valueOf(arg.get("forLinux")))) continue;
            }

            commands.add(convertArg(String.valueOf(arg.get("name"))));
        }

        String classpath = Arrays.stream(Objects.requireNonNull(new File(CommandUtil.getLib(), "engine").listFiles()))
                .map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator));

        commands.add("-Dqingzhou.instance=" + instanceDir.getAbsolutePath().replace("\\", "/"));
        commands.add("-classpath");
        commands.add(classpath);
        commands.add("qingzhou.engine.impl.Main");
    }

    private Map parseFileConfig() throws Exception {
        URL jsonUrl = Paths.get(CommandUtil.getLib().getAbsolutePath(), "module", "qingzhou-json.jar").toUri().toURL();
        URL engineUrl = Paths.get(CommandUtil.getLib().getAbsolutePath(), "engine", "qingzhou-engine.jar").toUri().toURL();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{engineUrl, jsonUrl})) {
            Class<?> loadedClass = classLoader.loadClass("qingzhou.json.impl.JsonImpl");
            Object instance = loadedClass.newInstance();
            Method fromJson = loadedClass.getMethod("fromJson", Reader.class, Class.class, String[].class);

            try (InputStream inputStream = Files.newInputStream(Paths.get(instanceDir.getAbsolutePath(), "conf", "qingzhou.json"))) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                return (Map) fromJson.invoke(instance, reader, Map.class, new String[]{"jvm"});
            }
        }
    }

    static String convertArg(String str) {
        if (str == null) {
            return null;
        }

        String convertBegin = "${";
        String convertEnd = "}";

        int begin = str.indexOf(convertBegin);
        if (begin < 0) {
            return str;
        }

        int end = str.indexOf(convertEnd, begin);
        if (end < 0) {
            return str;
        }

        String key = str.substring(begin + convertBegin.length(), end);
        String replacement = convert(key);

        String newStr = str.replace(convertBegin + key + convertEnd, replacement);
        return convertArg(newStr);
    }

    private static String convert(String origin) {
        String replacement = System.getProperty(origin);

        if (replacement == null) {
            replacement = System.getenv(origin);
        }

        if (replacement == null) {
            return "";
        }

        return replacement;
    }
}
