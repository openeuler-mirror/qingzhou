package qingzhou.command.server;

import qingzhou.command.CommandUtil;
import qingzhou.command.server.config.Arg;
import qingzhou.command.server.config.Env;
import qingzhou.command.server.config.Jvm;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private Jvm jvm;

    ConfigTool(File instanceDir) throws Exception {
        this.instanceDir = instanceDir;
        initConfig();
    }

    String getJavaHomeEnv() {
        for (Env env : this.jvm.getEnv()) {
            if (env.isEnabled() && env.getName().equals("JAVA_HOME")) {
                return env.getValue();
            }
        }
        return null;
    }

    Map<String, String> environment() {
        Map<String, String> environment = new HashMap<>();
        this.jvm.getEnv().forEach(env -> {
            if (env.isEnabled()) {
                environment.put(env.getName(), env.getValue());
            }
        });
        return environment;
    }

    List<String> command() {
        List<String> cmd = new ArrayList<>();
        this.jvm.getArg().forEach(arg -> {
            if (arg.isEnabled()) {
                if (arg.isForLinux() && CommandUtil.isWindows()) return;
                cmd.add(arg.getName());
            }
        });
        return cmd;
    }

    private void initConfig() throws Exception {
        Jvm jvm = parseFileConfig();

        String classpath = Arrays.stream(Objects.requireNonNull(new File(CommandUtil.getLibDir(), "engine").listFiles()))
                .map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator));
        jvm.getArg().add(new Arg("-classpath"));
        jvm.getArg().add(new Arg(classpath));
        jvm.getArg().add(new Arg("qingzhou.engine.impl.Main"));

        this.jvm = jvm;
    }

    private Jvm parseFileConfig() throws Exception {
        StringBuilder fileContent = new StringBuilder();
        try (InputStream inputStream = Files.newInputStream(new File(instanceDir, "jvm.json").toPath())) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            for (String line; (line = reader.readLine()) != null; ) {
                fileContent.append(line);
            }
        }
        String allContent = fileContent.toString().replace("${qingzhou.instance}", instanceDir.getAbsolutePath().replace("\\", "\\\\"));

        URL jsonURL = Paths.get(CommandUtil.getLibDir().getAbsolutePath(), "module", "qingzhou-json.jar").toUri().toURL();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jsonURL})) {
            Class<?> loadedClass = classLoader.loadClass("qingzhou.json.impl.JsonImpl");
            Object instance = loadedClass.newInstance();
            Method fromJson = loadedClass.getMethod("fromJson", String.class, Class.class);

            return (Jvm) fromJson.invoke(instance, allContent, Jvm.class);
        }
    }
}
