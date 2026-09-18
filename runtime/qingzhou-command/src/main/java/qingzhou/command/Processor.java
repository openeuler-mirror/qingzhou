package qingzhou.command;

import java.io.Console;
import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public abstract class Processor { // 兼容 老版本，因此动这个要小心
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

    protected void logSimple(String msg) {
        System.out.println(msg);
    }

    protected void log(String msg) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss >>> ");
        String logPrefix = dateFormat.format(new Date());
        System.out.println(logPrefix + msg);
    }

    // 读取密码：优先取命令行参数以兼容既有脚本；无参数时在交互终端下不回显读取，
    // 避免明文密码出现在 ps 输出中。非交互环境（重定向、后台执行）返回 null，由调用方提示用法，避免阻塞。
    protected String readPassword(String[] args) {
        if (args.length > 0 && !args[0].isEmpty()) {
            return args[0];
        }

        Console console = System.console();
        if (console == null) {
            return null;
        }

        char[] password = console.readPassword();
        return password == null ? null : new String(password);
    }

    protected File initInstance(String instanceName) {
        File instance = Paths.get(getHomeDir().getAbsolutePath(), "instances", instanceName).toFile();
        File configFile = Paths.get(instance.getAbsolutePath(), "conf", "qingzhou.properties").toFile();
        if (configFile.isFile()) {
            return instance;
        } else {
            log("instance does not exist: " + instanceName);
            return null;
        }
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
