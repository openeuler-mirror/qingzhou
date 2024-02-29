package qingzhou.framework.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.Map;

public class JDKUtil {
    private static Boolean isJdk9OrHigher;
    private static final String OS_NAME_PROPERTY = "os.name";

    public static final boolean IS_MAC_OS;
    public static final boolean IS_WINDOWS;
    public static final boolean IS_LINUX;

    static {
        // This check is derived from the check in Commons Lang
        String osName;
        if (System.getSecurityManager() == null) {
            osName = System.getProperty(OS_NAME_PROPERTY);
        } else {
            osName = AccessController.doPrivileged(
                    (PrivilegedAction<String>) () -> System.getProperty(OS_NAME_PROPERTY));
        }

        IS_MAC_OS = osName.toLowerCase(Locale.ENGLISH).startsWith("mac os x");

        IS_WINDOWS = osName.startsWith("Windows");

        IS_LINUX = !IS_MAC_OS && !IS_WINDOWS;
    }

    public static boolean isJdk9OrHigher() {
        if (isJdk9OrHigher == null) {
            double javaVerson = Double.parseDouble(System.getProperty("java.specification.version"));
            isJdk9OrHigher = javaVerson > 1.8;
        }
        return isJdk9OrHigher;
    }

    public static String javaCmd(Map<String, String> environment) {
        String javaHome = null;
        if (environment != null) {
            javaHome = environment.get("JAVA_HOME");
        }
        if (javaHome == null) {
            javaHome = System.getenv("JAVA_HOME");
        }
        javaHome = StringUtil.stripQuotes(javaHome);

        if (StringUtil.notBlank(javaHome) && new File(javaHome).isDirectory()) {
            return JDKUtil.getJavaBinTool("java", javaHome);
        }
        return "java";
    }

    public static int detectJavaVersion(String javaCmd) {
        try {
            String ver = getJavaVersionString(javaCmd);
            return parseJavaVersion(ver);
        } catch (IOException e) {
            return 8;
        }
    }

    public static int parseJavaVersion(String ver) {
        try {
            if (ver.startsWith("1.")) {
                ver = ver.substring(2);
            }
            int firstVer = ver.indexOf(".");
            if (firstVer > 0) {
                ver = ver.substring(0, firstVer);
            }
            return Integer.parseInt(ver);
        } catch (NumberFormatException e) {
            return 8;
        }
    }

    public static String getJavaBinTool(String cmd) {
        String javaHome = getJavaHome();
        if (StringUtil.isBlank(javaHome)) {
            return null;
        }
        return getJavaBinTool(cmd, javaHome);
    }

    public static String getJavaBinTool(String cmd, String javaHome) {
        //add for ITAIT-4712
        if (JDKUtil.IS_WINDOWS) {
            cmd += ".exe";
        }
        Path cmdPath = Paths.get(javaHome, "bin", cmd);
        return cmdPath.toString();
//        在 mac 本上，java 路径有空格时候，可以正常使用，不要向下面这样 用括号括起来哦
//        if (fullCmd.contains(" ")) {
//            fullCmd = "\"" + fullCmd + "\"";
//        }
    }

    public static String getJavaHome() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            return null;
        }
        File currentDir = new File(javaHome);
        if (!currentDir.isDirectory()) {
            return null;
        }

        String detectedJdkDir = currentDir.getParent();
        if (new File(detectedJdkDir, "bin").isDirectory()) {
            return detectedJdkDir;
        }

        if (new File(currentDir, "bin").isDirectory()) {
            return javaHome;
        }
        return null;
    }

    public static String getJavaVersionString(String javaCmd) throws IOException {
        if (SafeCheckerUtil.hasCmdInjectionRisk(javaCmd)) { // fix #ITAIT-4940
            throw new IllegalArgumentException("This command may have security risks: " + javaCmd);
        }
        String[] javaVersion = {null};
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(javaCmd);
        builder.command().add("-version");
        Process start = builder.start();
        StringCollector collectorUtil = null;
        try {
            collectorUtil = new StringCollector() {
                @Override
                public void collect(String line) {
                    super.collect(line);
                    String versionFlag = "version \"";
                    int i = line.toLowerCase().indexOf(versionFlag);
                    if (i != -1) {
                        line = line.substring(i + versionFlag.length());
                        int endFlag = line.indexOf("\"");
                        if (javaVersion[0] == null) {
                            javaVersion[0] = line.substring(0, endFlag);
                        }
                    }
                }
            };
            StreamUtil.readInputStreamWithThread(start.getInputStream(), collectorUtil, "stdout");
            StreamUtil.readInputStreamWithThread(start.getErrorStream(), collectorUtil, "stderr");
        } finally {
            try {
                start.waitFor();
            } catch (InterruptedException ignored) {
            }
            try {
                start.destroyForcibly();
            } catch (Exception ignored) {
            }
            if (collectorUtil != null) {
                collectorUtil.destroy();
            }
        }
        return javaVersion[0];
    }

    private JDKUtil() {
    }
}
