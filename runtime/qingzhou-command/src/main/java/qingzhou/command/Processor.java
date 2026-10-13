package qingzhou.command;

import java.io.Console;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public abstract class Processor {
    protected final boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");

    private File libDir;

    public File getLibDir() {
        return libDir;
    }

    public void setLibDir(File libDir) {
        this.libDir = libDir;
    }

    public File getHomeDir() {
        return libDir.getParentFile().getParentFile();
    }

    // 此命令行处理器的名字
    public abstract String name();

    public abstract String info();

    public String[] supportedArgs() {
        return null;
    }

    public abstract void doCommandLine(String[] args) throws Exception;

    protected void log(String msg) {
        System.out.println(msg);
    }

    // 读取密码：优先取命令行参数以兼容既有脚本（打印告警提醒泄露风险）；无参数时在交互终端下不回显读取，
    // 避免明文密码出现在 ps 输出中。非交互环境（重定向、后台执行）返回 null，由调用方提示用法，避免阻塞。
    protected String readPassword(String[] args) {
        if (args.length > 0 && !args[0].isEmpty()) {
            log("warning: password passed via command line is visible in 'ps' and shell history, interactive input is recommended");
            return args[0];
        }

        Console console = System.console();
        if (console == null) {
            return null;
        }

        char[] password = console.readPassword();
        if (password == null) return null;
        String result = new String(password);
        Arrays.fill(password, '\0'); // char[] 用后清零，减少明文驻留
        return result;
    }

    protected File initInstance(String[] args) {
        String instanceName = args.length > 0 && !args[0].isEmpty() ? args[0] : "default";
        if (!isSafeInstanceName(instanceName)) {
            log("invalid instance name: " + instanceName);
            return null;
        }
        File instance = Paths.get(getHomeDir().getAbsolutePath(), "instances", instanceName).toFile();
        File configFile = Paths.get(instance.getAbsolutePath(), "conf", "qingzhou.properties").toFile();
        if (configFile.isFile()) {
            // init 命令依赖全局密钥文件，全局密钥文件解析依赖系统属性
            System.setProperty("qingzhou.instance", instance.getAbsolutePath());
            return instance;
        } else {
            log("instance does not exist: " + instanceName);
            return null;
        }
    }

    // 实例名直接拼进路径，须拒绝任何能逃出 instances 目录的字符，否则可读写其它实例配置/密钥
    private static boolean isSafeInstanceName(String name) {
        if (name == null || name.isEmpty()) return false;
        return !name.contains("..") && !name.contains("/") && !name.contains("\\") && name.indexOf('\0') < 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Processor)) return false;
        Processor user = (Processor) o;
        return name().equals(user.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
