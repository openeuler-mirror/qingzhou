package qingzhou.console.util;

import qingzhou.framework.impl.ServerUtil;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;

public class NativeCommandUtil {
    private static volatile String pid;


    private NativeCommandUtil() {
    }

    public static String getPid() {
        if (NativeCommandUtil.pid == null) {
            RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
            String pid = bean.getName();
            if (pid.contains("@")) {
                pid = pid.substring(0, pid.indexOf('@'));

                NativeCommandUtil.pid = pid;
            }
        }

        return NativeCommandUtil.pid;
    }

    public static int runNativeCommand(String command, File dir, CollectorUtil output) throws Exception {
        return runNativeCommand(command, dir, output, 15);
    }

    public static int runNativeCommand(String command, File dir, CollectorUtil output, int waitSeconds) throws Exception {
        return runNativeCommand(command, dir, null, output, waitSeconds);
    }

    public static int runNativeCommand(String command, File dir, String[] envp, CollectorUtil output, int waitSeconds) throws Exception {
        if (output != null) {
            return runNativeCommand0(command, dir, envp, output, waitSeconds);
        } else {
            CollectorUtil collectorUtil = new CollectorUtil();
            try {
                int i = runNativeCommand0(command, dir, envp, collectorUtil, waitSeconds);
                String msg = collectorUtil.destroy();
                // for #ITAIT-5445 出错后方便定位问题
                if (StringUtil.notBlank(msg)) {
                    System.out.println(msg);
                }
                return i;
            } catch (Exception e) {
                System.out.println(collectorUtil.destroy());
                throw e;
            }
        }
    }

    private static int runNativeCommand0(String command, File dir, String[] envp, CollectorUtil output, int waitSeconds) throws Exception {
        if (SafeCheckerUtil.hasCmdInjectionRisk(command)) { // for #NC-1705
            throw new IllegalArgumentException("This command may have security risks: " + command);
        }

        String[] cmd;
        if (ServerUtil.isWindows()) {
            cmd = new String[3];
            cmd[0] = "cmd.exe";
            cmd[1] = "/c";
            cmd[2] = command;
        } else {
            if (command.contains(" ")) {
                cmd = new String[3];
                cmd[0] = "/bin/sh";
                cmd[1] = "-c";
                cmd[2] = command;
            } else {
                cmd = new String[2];
                cmd[0] = "/bin/sh";
                cmd[1] = command;
            }
        }

        Process process = Runtime.getRuntime().exec(cmd, envp, dir);

        // Consumes the stderr from the process
        StreamUtil.readInputStreamWithThread(process.getInputStream(), output, "stdout");
        StreamUtil.readInputStreamWithThread(process.getErrorStream(), output, "stderr");

        try {
            boolean ok = process.waitFor(waitSeconds, TimeUnit.SECONDS);
            if (ok) {
                return process.exitValue();
            } else {
                return -2;
            }
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    public static int forcedStop(String pid) throws Exception {
        return forcedStop(pid, null);
    }

    public static int forcedStop(String pid, CollectorUtil output) throws Exception {
        if (StringUtil.isBlank(pid)) {
            return -1;
        }
        if (ServerUtil.isWindows()) {
            return runNativeCommand(String.format("taskkill /f /pid %s", pid), null, output);
        } else {
            return runNativeCommand(String.format("kill -9 %s", pid), null, output);
        }
    }
}
