package qingzhou.command;

import java.util.Objects;

public abstract class CommandLineProcessor { // 兼容 老版本，因此动这个要小心
    /**
     * CommandLIne 会根据命令行处理器的名字分发对应的命令
     *
     * @return 此命令行处理器的名字
     */
    public abstract String name();

    public abstract String info();

    public String[] supportedArgs() {
        return null;
    }

    public abstract void doCommandLine(String[] args) throws Exception;

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
