package qingzhou.command;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public abstract class CommandLineProcessor { // 兼容 老版本，因此动这个要小心
    // 此命令行处理器的名字
    public abstract String name();

    public abstract String info();

    public String[] supportedArgs() {
        return null;
    }

    public abstract void doCommandLine(String[] args) throws Exception;

    protected void log(String msg) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss >>> ");
        String logPrefix = dateFormat.format(new Date());
        System.out.println(logPrefix + msg);
    }

    protected boolean initInstanceFailed(String instanceName) {
        String base = Paths.get(System.getProperty("qingzhou.home"), "instances", instanceName).toFile().getAbsolutePath();
        File configFile = Paths.get(base, "conf", "qingzhou.json").toFile();
        if (configFile.isFile()) {
            System.setProperty("qingzhou.instance", base);
            return false;
        } else {
            log("instance does not exist: " + instanceName);
            return true;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandLineProcessor)) return false;
        CommandLineProcessor user = (CommandLineProcessor) o;
        return name().equals(user.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
