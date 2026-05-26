package qingzhou.logger;

public interface Logger {
    void info(String msg);
    void info(String msg, Throwable e);
    void warn(String msg);
    void warn(String msg, Throwable e);
    void error(String msg);
    void error(String msg, Throwable e);
}