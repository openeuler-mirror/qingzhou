package qingzhou.command;

import java.io.File;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

public abstract class Processor {
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");

    private File libDir;

    protected File getLibDir() {
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

    public void log(String msg) {
        System.out.println(msg);
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
            return instance;
        } else {
            log("instance does not exist: " + instanceName);
            return null;
        }
    }

    // 实例名直接拼进路径，须拒绝任何能逃出 instances 目录的字符，否则可读写其它实例配置/密钥
    private static boolean isSafeInstanceName(String name) {
        if (name == null || name.isEmpty()) return false;
        return !name.equals(".") && !name.contains("..") && !name.contains("/") && !name.contains("\\") && name.indexOf('\0') < 0;
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
