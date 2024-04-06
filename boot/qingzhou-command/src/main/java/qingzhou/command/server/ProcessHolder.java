package qingzhou.command.server;

import qingzhou.command.CommandUtil;
import qingzhou.command.server.config.Jvm;
import qingzhou.command.server.config.QZXml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

class ProcessHolder {
    private final List<String> cmd;
    private final File instanceDir;
    private ProcessBuilder processBuilder;
    private Process process;

    ProcessHolder(File instanceDir, List<String> cmd) {
        this.instanceDir = instanceDir;
        this.cmd = cmd;
    }

    ProcessBuilder buildProcess() throws Exception {
        if (processBuilder != null) return processBuilder;

        Jvm javaConfig = new QZXml(instanceDir).buildJavaConfig();

        // prepare javaCmd
        String javaCmd = javaCmd(javaConfig.environment());
        String javaVersion;
        try {
            javaVersion = CommandUtil.getJavaVersionString(javaCmd);
        } catch (Exception e) {
            javaVersion = "1.8.0";

            CommandUtil.log("javaCmd: " + javaCmd);
            CommandUtil.log("warning: " + e.getMessage());
            CommandUtil.log("Failed to parse the Java version number. Continue with " + javaVersion);
        }

        if (javaVersion != null) {
            String ver = javaVersion.trim();
            if (!ver.isEmpty()) {
                // log("Using java version: " + ver);
                javaConfig.prepare(ver);
            }
        }

        // build ProcessBuilder
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(instanceDir);
        builder.environment().putAll(javaConfig.environment());
        builder.command(javaCmd);
        builder.command().addAll(javaConfig.command());
        for (String cmd : cmd) {
            builder.command().add(cmd);
        }
        processBuilder = builder;

        return processBuilder;
    }

    Process startProcess() throws IOException {
        if (process != null) return process;

        for (String c : processBuilder.command()) {
            if (CommandUtil.hasCmdInjectionRisk(c)) { // fix #ITAIT-4940
                throw new IllegalArgumentException("This command may have security risks: " + c);
            }
        }
        process = processBuilder.start();
        return process;
    }


    private String javaCmd(Map<String, String> environment) {
        String javaHome = null;
        if (environment != null) {
            javaHome = environment.get("JAVA_HOME");
        }
        if (javaHome == null) {
            javaHome = System.getenv("JAVA_HOME");
        }
        javaHome = CommandUtil.stripQuotes(javaHome);

        if (CommandUtil.notBlank(javaHome) && new File(javaHome).isDirectory()) {
            return javaCmdInDir(javaHome);
        }
        return "java";
    }

    private String javaCmdInDir(String javaHomeDir) {
        String cmd = "java";
        if (CommandUtil.isWindows()) {
            cmd += ".exe";
        }
        Path cmdPath = Paths.get(javaHomeDir, "bin", cmd);
        return cmdPath.toString();
    }
}
