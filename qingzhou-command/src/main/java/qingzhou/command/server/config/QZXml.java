package qingzhou.command.server.config;

import qingzhou.command.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

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

    private final File qzXml;
    private final File domain;

    public QZXml(File qzXml) {
        this.qzXml = qzXml;
        this.domain = qzXml.getParentFile().getParentFile();
    }

    public Jvm buildJavaConfig() throws Exception {
        Jvm javaConfig = parseJavaConfig();
        javaConfig.setWorkDir(convertArg(javaConfig.getWorkDir()));
        for (Env env : javaConfig.getEnvs()) {
            env.setValue(convertArg(env.getValue()));
        }
        for (Arg arg : javaConfig.getArgs()) {
            arg.setName(convertArg(arg.getName()));
        }

        return javaConfig;
    }

    private Jvm parseJavaConfig() throws Exception {
        Jvm javaConfig = new Jvm();
        Map<String, String> attributes = Utils.getAttributes(Utils.getServerXml(Utils.getDomain()), "//jvm");
        Utils.setObjectValues(javaConfig, attributes);

        List<Map<String, String>> envS = Utils.getAttributesList(qzXml, "//jvm/environments/env");
        if (envS != null) {
            for (Map<String, String> p : envS) {
                String s = p.get("enabled");
                if (Boolean.parseBoolean(Utils.notBlank(s) ? s : "true")) {
                    Env env = new Env();
                    env.setName(p.get("name"));
                    env.setValue(p.get("value"));
                    javaConfig.addEnv(env);
                }
            }
        }

        List<Map<String, String>> args = Utils.getAttributesList(qzXml, "//jvm/args/arg");
        if (args != null) {
            for (Map<String, String> p : args) {
                String s = p.get("enabled");
                if (Boolean.parseBoolean(Utils.notBlank(s) ? s : "true")) {
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
        File[] otherJars = new File(Utils.getLibDir(), "bootstrap").listFiles();
        List<File> fileList = Arrays.asList(Objects.requireNonNull(otherJars));
        String classpath = Utils.join(fileList, File.pathSeparator);

        javaConfig.addArg(new Arg("-classpath"));
        javaConfig.addArg(new Arg(classpath));
        javaConfig.addArg(new Arg("qingzhou.bootstrap.main.Main"));

        return javaConfig;
    }

    private String convertArg(String str) throws Exception {
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
        String replacement = convert(key, domain.getCanonicalPath(), Utils.getHome().getCanonicalPath());

        String newStr = str.replace(convertBegin + key + convertEnd, replacement);
        return convertArg(newStr);
    }

    private String convert(String origin, String qzDomain, String qzHome) {

        if ("qingzhou.domain".equals(origin)) {
            return qzDomain;
        }

        if ("qingzhou.home".equals(origin)) {
            return qzHome;
        }

        if ("QZ_TimeStamp".equals(origin)) {
            return new SimpleDateFormat("yyyy-MM-dd'T'HHmmss").format(new Date());
        }

        String replacement = System.getProperty(origin);

        if (replacement == null) {
            replacement = System.getenv(origin);
        }

        if (replacement == null) {
            throw new IllegalStateException(origin + " not found in env or -D args!");
        }
        return replacement;
    }
}
