package qingzhou.bootstrap.command.impl.server;

import qingzhou.bootstrap.Utils;
import qingzhou.bootstrap.command.impl.server.config.Jvm;
import qingzhou.bootstrap.command.impl.server.config.QZXml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

class ProcessHolder {
    public static boolean checkIfStarted(File domain) {
        File pidFile = serverPidFile(domain);
        if (!pidFile.exists()) {
            return false;
        }

        // 如果被kill上，上述判断会不准确
        Map<String, String> attributes = Utils.getAttributes(Utils.getServerXml(Utils.getDomain()), "//server/connector");
        int serverPort = Integer.parseInt(attributes.get("port"));
        return Utils.isPortOpened("localhost", serverPort);
    }

    public static File serverPidFile(File domain) {
        return Utils.getCache(domain, "server.pid");
    }

    private final File serverXml;
    private final List<String> cmds;
    private ProcessBuilder processBuilder;
    private Process process;

    ProcessHolder(File serverXml, List<String> cmds) {
        this.serverXml = serverXml;
        this.cmds = cmds;
    }

    ProcessBuilder buildProcess() throws Exception {
        if (processBuilder != null) return processBuilder;

        Jvm javaConfig = new QZXml(serverXml).buildJavaConfig();

        // prepare javaCmd
        String javaCmd = javaCmd(javaConfig.environment());
        String javaVersion;
        try {
            javaVersion = Utils.getJavaVersionString(javaCmd);
        } catch (Exception e) {
            javaVersion = "1.8.0";

            Server.log("javaCmd: " + javaCmd);
            Server.log("warning: " + e.getMessage());
            Server.log("Failed to parse the Java version number. Continue with " + javaVersion);
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
        builder.directory(new File(javaConfig.getWorkDir()));
        builder.environment().putAll(javaConfig.environment());
        builder.command(javaCmd);
        builder.command().addAll(javaConfig.command());
        for (String cmd : cmds) {
            builder.command().add(cmd);
        }
        processBuilder = builder;

        return processBuilder;
    }

    Process startProcess() throws IOException {
        if (process != null) return process;

        for (String c : processBuilder.command()) {
            if (Utils.hasCmdInjectionRisk(c)) { // fix #ITAIT-4940
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
        javaHome = Utils.stripQuotes(javaHome);

        if (Utils.notBlank(javaHome) && new File(javaHome).isDirectory()) {
            return javaCmdInDir(javaHome);
        }
        return "java";
    }

    private String javaCmdInDir(String javaHomeDir) {
        String cmd = "java";
        if (Utils.isWindows()) {
            cmd += ".exe";
        }
        Path cmdPath = Paths.get(javaHomeDir, "bin", cmd);
        return cmdPath.toString();
    }
}
