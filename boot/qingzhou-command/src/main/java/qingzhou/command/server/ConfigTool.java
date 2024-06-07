package qingzhou.command.server;

import qingzhou.command.CommandUtil;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

class ConfigTool {
    private final File instanceDir;
    private final List<String> commands = new ArrayList<>();
    private final Map<String, String> envs = new HashMap<>();

    ConfigTool(File instanceDir) throws Exception {
        this.instanceDir = instanceDir;
        initConfig();
    }

    String getJavaHomeEnv() {
        return envs.get("JAVA_HOME");
    }

    Map<String, String> environment() {
        return envs;
    }

    List<String> command() {
        return commands;
    }

    private void initConfig() throws Exception {
        Map jvm = parseFileConfig();

        Map[] envs = (Map[]) jvm.get("arg");
        for (Map env : envs) {
            if (Boolean.parseBoolean(String.valueOf(env.get("enabled")))) {
                this.envs.put(String.valueOf(env.get("name")),
                        String.valueOf(env.get("value")));
            }
        }

        Map[] args = (Map[]) jvm.get("arg");
        for (Map arg : args) {
            if (!Boolean.parseBoolean(String.valueOf(arg.get("enabled")))) continue;
            if (CommandUtil.isWindows()) {
                if (Boolean.parseBoolean(String.valueOf(arg.get("forLinux")))) continue;
            }

            if (arg.get("supportedJRE") != null) {
                String supportedJRE = String.valueOf(arg.get("supportedJRE"));
                if (!supportedJRE.isEmpty()) {
                    if (!isVerMatches(supportedJRE)) {
                        continue;
                    }
                }
            }

            commands.add(String.valueOf(arg.get("name")));
        }

        String classpath = Arrays.stream(Objects.requireNonNull(new File(CommandUtil.getLibDir(), "engine").listFiles()))
                .map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator));

        commands.add("-Dqingzhou.instance=" + instanceDir.getAbsolutePath().replace("\\", "/"));
        commands.add("-classpath");
        commands.add(classpath);
        commands.add("qingzhou.engine.impl.Main");
    }

    private static boolean isVerMatches(String supportedJRE) {
        int jreVer;
        boolean minus = supportedJRE.endsWith("-");
        boolean plus = supportedJRE.endsWith("+");
        if (minus || plus) {
            jreVer = Integer.parseInt(supportedJRE.substring(0, supportedJRE.length() - 1));
        } else {
            jreVer = Integer.parseInt(supportedJRE);
        }

        String ver = System.getProperty("java.specification.version");
        if (ver != null && !ver.isEmpty()) {
            int currentJreVer = parseJavaVersion(ver);
            if (minus) {
                if (currentJreVer > jreVer) {
                    return false;
                }
            }
            if (plus) {
                if (currentJreVer < jreVer) {
                    return false;
                }
            }
            if (!minus && !plus) {
                return currentJreVer == jreVer;
            }
        }

        return true;
    }

    private static int parseJavaVersion(String ver) {
        try {
            if (ver.startsWith("1.")) {
                ver = ver.substring(2);
            }
            int firstVer = ver.indexOf(".");
            if (firstVer > 0) {
                ver = ver.substring(0, firstVer);
            }
            return Integer.parseInt(ver);
        } catch (Exception e) {
            return 8;
        }
    }

    private Map parseFileConfig() throws Exception {
        URL jsonURL = Paths.get(CommandUtil.getLibDir().getAbsolutePath(), "module", "qingzhou-json.jar").toUri().toURL();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jsonURL})) {
            Class<?> loadedClass = classLoader.loadClass("qingzhou.json.impl.JsonImpl");
            Object instance = loadedClass.newInstance();
            Method fromJson = loadedClass.getMethod("fromJson", Reader.class, Class.class, String[].class);

            try (InputStream inputStream = Files.newInputStream(new File(instanceDir, "qingzhou.json").toPath())) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                return (Map) fromJson.invoke(instance, reader, Map.class, new String[]{"jvm"});
            }
        }
    }
}
