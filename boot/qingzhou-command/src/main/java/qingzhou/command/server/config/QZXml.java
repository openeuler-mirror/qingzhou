package qingzhou.command.server.config;

import qingzhou.command.CommandUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class QZXml {
    private final String[] JAVA_9_PLUS = new String[]{"--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-opens=java.base/java.math=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
            "--add-opens=java.base/java.net=ALL-UNNAMED",
            "--add-opens=java.base/java.text=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED", // for OpenRasp的agent
            "--add-opens=java.base/sun.security.action=ALL-UNNAMED", // for #ITAIT-5445
            "--add-opens=java.sql/java.sql=ALL-UNNAMED",
            "--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED",
            "--add-opens=java.management.rmi/javax.management.remote.rmi=ALL-UNNAMED",
            "--add-exports=java.management/com.sun.jmx.remote.security=ALL-UNNAMED"};

    private final File instanceDir;

    public QZXml(File instanceDir) {
        this.instanceDir = instanceDir;
    }

    public Jvm buildJavaConfig() throws Exception {
        Jvm javaConfig = parseJavaConfig();
        for (Env env : javaConfig.getEnvs()) {
            env.setValue(convertArg(env.getValue()));
        }
        for (Arg arg : javaConfig.getArgs()) {
            arg.setName(convertArg(arg.getName()));
        }

        return javaConfig;
    }

    private Jvm parseJavaConfig() throws Exception {
        File qzXml = new File(instanceDir, "qingzhou.xml");

        Jvm javaConfig = new Jvm();
        Map<String, String> attributes = CommandUtil.getAttributes(qzXml, "//jvm");
        CommandUtil.setObjectValues(javaConfig, attributes);

        List<Map<String, String>> envS = CommandUtil.getAttributesList(qzXml, "//jvm/envs/env");
        if (envS != null) {
            for (Map<String, String> p : envS) {
                String s = p.get("enabled");
                if (Boolean.parseBoolean(CommandUtil.notBlank(s) ? s : "true")) {
                    Env env = new Env();
                    env.setName(p.get("name"));
                    env.setValue(p.get("value"));
                    javaConfig.addEnv(env);
                }
            }
        }

        List<Map<String, String>> args = CommandUtil.getAttributesList(qzXml, "//jvm/args/arg");
        if (args != null) {
            for (Map<String, String> p : args) {
                String s = p.get("enabled");
                if (Boolean.parseBoolean(CommandUtil.notBlank(s) ? s : "true")) {
                    Arg arg = new Arg(p.get("name"), Boolean.parseBoolean(p.get("onlyForLinux")), p.get("supportedJRE"));
                    javaConfig.addArg(arg);
                }
            }
        }

        // 支持高版本 jdk
        for (String addArg : JAVA_9_PLUS) {
            Arg extJdkArg = new Arg(addArg, false, "16+");
            boolean exists = false;
            for (Arg arg : javaConfig.getArgs()) {
                if (arg.getName().equals(extJdkArg.getName())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                javaConfig.addArg(extJdkArg);
            }
        }

        // 启动类
        File[] otherJars = new File(CommandUtil.getLibDir(), "engine").listFiles();
        List<File> fileList = Arrays.asList(Objects.requireNonNull(otherJars));
        String classpath = CommandUtil.join(fileList, File.pathSeparator);

        javaConfig.addArg(new Arg("-classpath"));
        javaConfig.addArg(new Arg(classpath));
        javaConfig.addArg(new Arg("qingzhou.engine.main.Main"));

        return javaConfig;
    }

    private String convertArg(String str) throws Exception {
        if (str == null) {
            return null;
        }

        return str.replace("${qingzhou.instance}", instanceDir.getCanonicalPath());
    }
}
