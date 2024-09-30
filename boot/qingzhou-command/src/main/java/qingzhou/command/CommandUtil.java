package qingzhou.command;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CommandUtil {
    public static File getLibDir() {
        String jarPath = Admin.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            // 兼容中文路径
            jarPath = java.net.URLDecoder.decode(
                    jarPath,
                    Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String flag = "/command/qingzhou-command.jar";
        int i = jarPath.indexOf(flag);
        String pre = jarPath.substring(0, i);
        return new File(pre);
    }

    public static void log(String msg) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss >>> ");
        String logPrefix = dateFormat.format(new Date());
        System.out.println(logPrefix + msg);
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

    public static String stripQuotes(String value) {
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
