package qingzhou.command;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class CommandUtil {
    public static File getHome() {
        return new File(System.getProperty("qingzhou.home"));
    }

    public static File getInstance() {
        return new File(System.getProperty("qingzhou.instance"));
    }

    public static File getLib() {
        return new File(System.getProperty("qingzhou.lib"));
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "unknown").toLowerCase(Locale.ENGLISH).startsWith("win");
    }

    public static String getJavaCmd(String javaHome) {
        if (javaHome == null) {
            javaHome = System.getenv("JAVA_HOME");
        }
        javaHome = CommandUtil.stripQuotes(javaHome);

        if (javaHome != null && new File(javaHome).isDirectory()) {
            String cmd = "java";
            if (CommandUtil.isWindows()) {
                cmd += ".exe";
            }
            Path cmdPath = Paths.get(javaHome, "bin", cmd);
            return cmdPath.toString();
        }

        return "java";
    }

    static String stripQuotes(String value) {
        if (value == null) return null;

        while (value.startsWith("\"")) {
            value = value.substring(1);
        }
        while (value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
